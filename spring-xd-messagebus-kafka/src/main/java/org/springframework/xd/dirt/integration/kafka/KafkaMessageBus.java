/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.integration.kafka;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import kafka.admin.AdminUtils;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.javaapi.producer.Producer;
import kafka.producer.DefaultPartitioner;
import kafka.producer.ProducerConfig;
import kafka.serializer.Decoder;
import kafka.serializer.DefaultDecoder;
import kafka.serializer.DefaultEncoder;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.ZkSerializer;
import scala.collection.Seq;

import org.springframework.context.Lifecycle;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.kafka.support.ProducerConfiguration;
import org.springframework.integration.kafka.support.ProducerFactoryBean;
import org.springframework.integration.kafka.support.ProducerMetadata;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.xd.dirt.integration.bus.AbstractBusPropertiesAccessor;
import org.springframework.xd.dirt.integration.bus.Binding;
import org.springframework.xd.dirt.integration.bus.BusProperties;
import org.springframework.xd.dirt.integration.bus.EmbeddedHeadersMessageConverter;
import org.springframework.xd.dirt.integration.bus.MessageBusSupport;
import org.springframework.xd.dirt.integration.bus.serializer.MultiTypeCodec;

/**
 * A message bus that uses Kafka as the underlying middleware.
 *
 * The general implementation mapping between XD concepts and Kafka concepts is as follows:
 * <table>
 *     <tr>
 *         <th>Stream definition</th><th>Kafka topic</th><th>Kafka partitions</th><th>Notes</th>
 *     </tr>
 *     <tr>
 *         <td>foo = "http | log"</td><td>foo.0</td><td>1 partition</td><td>1 producer, 1 consumer</td>
 *     </tr>
 *     <tr>
 *         <td>foo = "http | log", log.count=x</td><td>foo.0</td><td>x partitions</td><td>1 producer, x consumers with static group 'springXD', achieves queue semantics</td>
 *     </tr>
 *     <tr>
 *         <td>foo = "http | log", log.count=x + XD partitioning</td><td>still 1 topic 'foo.0'</td><td>x partitions + use key computed by XD</td><td>1 producer, x consumers with static group 'springXD', achieves queue semantics</td>
 *     </tr>
 *     <tr>
 *         <td>foo = "http | log", log.count=x, concurrency=y</td><td>foo.0</td><td>x*y partitions</td><td>1 producer, x XD consumers, each with y threads</td>
 *     </tr>
 *     <tr>
 *         <td>foo = "http | log", log.count=0, x actual log containers</td><td>foo.0</td><td>10(configurable) partitions</td><td>1 producer, x XD consumers. Can't know the number of partitions beforehand, so decide a number that better be greater than number of containers</td>
 *     </tr>
 * </table>
 *
 * @author Eric Bottard
 * @author Marius Bogoevici
 */
public class KafkaMessageBus extends MessageBusSupport {

	public static final String COMPRESSION_CODEC = "compressionCodec";

	public static final String REQUIRED_ACKS = "requiredAcks";

	/**
	 * Used when writing directly to ZK. This is what Kafka expects.
	 */
	public final static ZkSerializer utf8Serializer = new ZkSerializer() {

		@Override
		public byte[] serialize(Object data) throws ZkMarshallingError {
			try {
				return ((String) data).getBytes("UTF-8");
			}
			catch (UnsupportedEncodingException e) {
				throw new ZkMarshallingError(e);
			}
		}

		@Override
		public Object deserialize(byte[] bytes) throws ZkMarshallingError {
			try {
				return new String(bytes, "UTF-8");
			}
			catch (UnsupportedEncodingException e) {
				throw new ZkMarshallingError(e);
			}
		}
	};

	protected static final Set<Object> PRODUCER_COMPRESSION_PROPERTIES = new HashSet<Object>(
			Arrays.asList(new String[] {
					KafkaMessageBus.COMPRESSION_CODEC,
			}));

	private static final String XD_REPLY_CHANNEL = "xdReplyChannel";

	/**
	 * The consumer group to use when achieving point to point semantics (that
	 * consumer group name is static and hence shared by all containers).
	 */
	private static final String POINT_TO_POINT_SEMANTICS_CONSUMER_GROUP = "springXD";

	/**
	 * The headers that will be propagated, by default.
	 */
	private static final String[] STANDARD_HEADERS = new String[] {
			IntegrationMessageHeaderAccessor.CORRELATION_ID,
			IntegrationMessageHeaderAccessor.SEQUENCE_SIZE,
			IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER,
			MessageHeaders.CONTENT_TYPE,
			ORIGINAL_CONTENT_TYPE_HEADER,
			XD_REPLY_CHANNEL
	};

	/**
	 * Basic + concurrency + partitioning.
	 */
	private static final Set<Object> SUPPORTED_CONSUMER_PROPERTIES = new SetBuilder()
			.add(BusProperties.PARTITION_INDEX) // Not actually used
			.add(BusProperties.CONCURRENCY)
			.build();

	/**
	 * Basic + concurrency.
	 */
	private static final Set<Object> SUPPORTED_NAMED_CONSUMER_PROPERTIES = new SetBuilder()
			.build();

	private static final Set<Object> SUPPORTED_NAMED_PRODUCER_PROPERTIES = new SetBuilder()
			.addAll(PRODUCER_STANDARD_PROPERTIES)
			.addAll(PRODUCER_BATCHING_BASIC_PROPERTIES)
			.build();

	/**
	 * Partitioning + kafka producer properties.
	 */
	private static final Set<Object> SUPPORTED_PRODUCER_PROPERTIES = new SetBuilder()
			.addAll(PRODUCER_PARTITIONING_PROPERTIES)
			.addAll(PRODUCER_STANDARD_PROPERTIES)
			.add(BusProperties.DIRECT_BINDING_ALLOWED)
			.addAll(PRODUCER_BATCHING_BASIC_PROPERTIES)
			.addAll(PRODUCER_COMPRESSION_PROPERTIES)
			.build();

	private final EmbeddedHeadersMessageConverter embeddedHeadersMessageConverter = new EmbeddedHeadersMessageConverter();

	private String brokers;

	private ExecutorService executor = Executors.newCachedThreadPool();

	private String[] headersToMap;

	private String zkAddress;

	// -------- Default values for properties -------
	private int defaultReplicationFactor = 1;

	private String defaultCompressionCodec = "default";

	private int defaultRequiredAcks = 1;

	/**
	 * The number of Kafka partitions to use when module count can auto-grow.
	 * Should be bigger than number of containers that will ever exist.
	 */
	private int numOfKafkaPartitionsForCountEqualsZero = 10;



	public KafkaMessageBus(String brokers, String zkAddress, MultiTypeCodec<Object> codec, String... headersToMap) {
		this.brokers = brokers;
		this.zkAddress = zkAddress;
		setCodec(codec);
		if (headersToMap.length > 0) {
			String[] combinedHeadersToMap =
					Arrays.copyOfRange(STANDARD_HEADERS, 0, STANDARD_HEADERS.length + headersToMap.length);
			System.arraycopy(headersToMap, 0, combinedHeadersToMap, STANDARD_HEADERS.length, headersToMap.length);
			this.headersToMap = combinedHeadersToMap;
		}
		else {
			this.headersToMap = STANDARD_HEADERS;
		}

	}

	/**
	 * Allowed chars are ASCII alphanumerics, '.', '_' and '-'.
	 * '_' is used as escaped char in the form '_xx' where xx is the hexadecimal
	 * value of the byte(s) needed to represent an illegal char in utf8.
	 */
	/*default*/
	static String escapeTopicName(String original) {
		StringBuilder result = new StringBuilder(original.length());
		try {
			byte[] utf8 = original.getBytes("UTF-8");
			for (byte b : utf8) {
				if ((b >= 'a') && (b <= 'z') || (b >= 'A') && (b <= 'Z') || (b >= '0') && (b <= '9') || (b == '.')
						|| (b == '-')) {
					result.append((char) b);
				}
				else {
					result.append(String.format("_%02X", b));
				}
			}
		}
		catch (UnsupportedEncodingException e) {
			throw new AssertionError(e); // Can't happen
		}
		return result.toString();
	}

	public void setDefaultReplicationFactor(int defaultReplicationFactor) {
		this.defaultReplicationFactor = defaultReplicationFactor;
	}

	public void setDefaultCompressionCodec(String defaultCompressionCodec) {
		this.defaultCompressionCodec = defaultCompressionCodec;
	}

	public void setDefaultRequiredAcks(int defaultRequiredAcks) {
		this.defaultRequiredAcks = defaultRequiredAcks;
	}

	@Override
	public void bindConsumer(String name, final MessageChannel moduleInputChannel, Properties properties) {
		createKafkaConsumer(name, moduleInputChannel, properties,
				createConsumerConnector(POINT_TO_POINT_SEMANTICS_CONSUMER_GROUP));
		bindExistingProducerDirectlyIfPossible(name, moduleInputChannel);

	}

	@Override
	public void bindPubSubConsumer(String name, MessageChannel inputChannel, Properties properties) {
		// Usage of a different consumer group each time achieves pub-sub
		String group = UUID.randomUUID().toString();
		createKafkaConsumer(name, inputChannel, properties, createConsumerConnector(group));
	}

	@Override
	public void bindProducer(final String name, MessageChannel moduleOutputChannel, Properties properties) {

		Assert.isInstanceOf(SubscribableChannel.class, moduleOutputChannel);
		KafkaPropertiesAccessor accessor = new KafkaPropertiesAccessor(properties);
		if (name.startsWith(P2P_NAMED_CHANNEL_TYPE_PREFIX)) {
			validateProducerProperties(name, properties, SUPPORTED_NAMED_PRODUCER_PROPERTIES);
		}
		else {
			validateProducerProperties(name, properties, SUPPORTED_PRODUCER_PROPERTIES);
		}
		if (!bindNewProducerDirectlyIfPossible(name, (SubscribableChannel) moduleOutputChannel, accessor)) {
			if (logger.isInfoEnabled()) {
				logger.info("Using kafka topic for outbound: " + name);
			}


			final String topicName = escapeTopicName(name);
			int numPartitions = accessor.getNumberOfKafkaPartitions();

			ensureTopicCreated(topicName, numPartitions, defaultReplicationFactor);




			ProducerMetadata<Integer, byte[]> producerMetadata = new ProducerMetadata<Integer, byte[]>(
					topicName);
			producerMetadata.setValueEncoder(new DefaultEncoder(null));
			producerMetadata.setValueClassType(byte[].class);
			producerMetadata.setKeyEncoder(new IntegerEncoderDecoder(null));
			producerMetadata.setKeyClassType(Integer.class);
			producerMetadata.setCompressionCodec(accessor.getCompressionCodec(this.defaultCompressionCodec));
			producerMetadata.setPartitioner(new DefaultPartitioner(null));

			Properties additionalProps = new Properties();
			additionalProps.put("request.required.acks", String.valueOf(accessor.getRequiredAcks(this.defaultRequiredAcks)));
			if (accessor.isBatchingEnabled(this.defaultBatchingEnabled)) {
				producerMetadata.setAsync(true);
				producerMetadata.setBatchNumMessages(String.valueOf(accessor.getBatchSize(this.defaultBatchSize)));
				additionalProps.put("queue.buffering.max.ms", String.valueOf(accessor.getBatchTimeout(this.defaultBatchTimeout)));
			}

			ProducerFactoryBean<Integer, byte[]> producerFB = new ProducerFactoryBean<Integer, byte[]>(producerMetadata, brokers, additionalProps);

			try {
				final Producer<Integer, byte[]> producer = producerFB.getObject();


				final ProducerConfiguration<Integer, byte[]> producerConfiguration = new ProducerConfiguration<Integer, byte[]>(
						producerMetadata, producer);

				MessageHandler messageHandler = new AbstractMessageHandler() {

					@Override
					protected void handleMessageInternal(Message<?> message) throws Exception {
						producerConfiguration.send(topicName, null, message);
					}
				};

				MessageHandler handler = new SendingHandler(messageHandler, topicName, accessor);
				EventDrivenConsumer consumer = new EventDrivenConsumer((SubscribableChannel) moduleOutputChannel, handler);
				consumer.setBeanFactory(this.getBeanFactory());
				consumer.setBeanName("outbound." + name);
				consumer.afterPropertiesSet();
				Binding producerBinding = Binding.forProducer(name, moduleOutputChannel, consumer, accessor);
				addBinding(producerBinding);
				producerBinding.start();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}

		}

	}

	@Override
	public void bindPubSubProducer(String name, MessageChannel outputChannel, Properties properties) {
		bindProducer(name, outputChannel, properties);
	}

	@Override
	public void bindRequestor(String name, MessageChannel requests, MessageChannel replies, Properties properties) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public void bindReplier(String name, MessageChannel requests, MessageChannel replies, Properties properties) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * Creates a Kafka topic if needed, or try to increase its partition count to the desired number.
	 */
	private void ensureTopicCreated(final String topicName, int numPartitions, int replicationFactor) {
		final int sessionTimeoutMs = 10000;
		final int connectionTimeoutMs = 10000;
		ZkClient zkClient = new ZkClient(zkAddress, sessionTimeoutMs, connectionTimeoutMs, utf8Serializer);

		// The following is basically copy/paste from AdminUtils.createTopic() with
		// createOrUpdateTopicPartitionAssignmentPathInZK(..., update=true)
		Properties topicConfig = new Properties();
		Seq<Object> brokerList = ZkUtils.getSortedBrokerList(zkClient);
		scala.collection.Map<Object, Seq<Object>> replicaAssignment = AdminUtils.assignReplicasToBrokers(brokerList,
				numPartitions, replicationFactor, -1, -1);
		AdminUtils.createOrUpdateTopicPartitionAssignmentPathInZK(zkClient, topicName, replicaAssignment, topicConfig,
				true);
		zkClient.close();

	}

	private void createKafkaConsumer(String name, final MessageChannel moduleInputChannel, Properties properties,
			ConsumerConnector connector) {

		if (name.startsWith(P2P_NAMED_CHANNEL_TYPE_PREFIX)) {
			validateConsumerProperties(name, properties, SUPPORTED_NAMED_CONSUMER_PROPERTIES);
		}
		else {
			validateConsumerProperties(name, properties, SUPPORTED_CONSUMER_PROPERTIES);
		}
		KafkaPropertiesAccessor accessor = new KafkaPropertiesAccessor(properties);

		Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
		int numThreads = accessor.getConcurrency(defaultConcurrency);
		String topic = escapeTopicName(name);
		topicCountMap.put(topic, numThreads);


		Decoder<byte[]> valueDecoder = new DefaultDecoder(null);
		Decoder<Integer> keyDecoder = new IntegerEncoderDecoder();
		Map<String, List<KafkaStream<Integer, byte[]>>> consumerMap = connector.createMessageStreams(
				topicCountMap, keyDecoder, valueDecoder);

		final KafkaStream<Integer, byte[]> stream = consumerMap.get(topic).iterator().next();

		final DirectChannel bridge = new DirectChannel();
		ReceivingHandler rh = new ReceivingHandler(connector);
		rh.setOutputChannel(moduleInputChannel);
		EventDrivenConsumer edc = new EventDrivenConsumer(bridge, rh);
		edc.setBeanName("inbound." + name);

		Binding consumerBinding = Binding.forConsumer(name, edc, moduleInputChannel, accessor);
		addBinding(consumerBinding);
		consumerBinding.start();


		executor.submit(new Runnable() {

			@Override
			public void run() {
				ConsumerIterator<Integer, byte[]> it = stream.iterator();
				while (it.hasNext()) {
					byte[] msg = it.next().message();
					bridge.send(MessageBuilder.withPayload(msg).build());
				}
			}

		});
	}

	/*default*/ConsumerConnector createConsumerConnector(String consumerGroup, String... keyValues) {
		Properties props = new Properties();
		props.put("zookeeper.connect", zkAddress);
		props.put("group.id", consumerGroup);
		Assert.isTrue(keyValues.length % 2 == 0, "keyValues must be an even number of key/value pairs");
		for (int i = 0; i < keyValues.length; i += 2) {
			String key = keyValues[i];
			String value = keyValues[i + 1];
			props.put(key, value);
		}
		ConsumerConfig config = new ConsumerConfig(props);
		return Consumer.createJavaConsumerConnector(config);
	}

	private class KafkaPropertiesAccessor extends AbstractBusPropertiesAccessor {

		public KafkaPropertiesAccessor(Properties properties) {
			super(properties);
		}

		public int getNumberOfKafkaPartitions() {
			int concurrency = getProperty(NEXT_MODULE_CONCURRENCY, defaultConcurrency);
			if (new PartitioningMetadata(this).isPartitionedModule()) {
				return getPartitionCount() * concurrency;
			}
			else {
				int downStreamModuleCount = getProperty(NEXT_MODULE_COUNT, 1);
				int base = downStreamModuleCount == 0 ? numOfKafkaPartitionsForCountEqualsZero : downStreamModuleCount;
				return base * concurrency;
			}
		}

		public String getCompressionCodec(String defaultValue) {
			return getProperty(COMPRESSION_CODEC, defaultValue);
		}

		public int getRequiredAcks(int defaultRequiredAcks) {
			return getProperty(REQUIRED_ACKS, defaultRequiredAcks);
		}
	}

	private class ReceivingHandler extends AbstractReplyProducingMessageHandler implements Lifecycle {

		private ConsumerConnector connector;

		public ReceivingHandler(ConsumerConnector connector) {
			this.connector = connector;
			this.setBeanFactory(KafkaMessageBus.this.getBeanFactory());
		}

		@Override
		@SuppressWarnings("unchecked")
		protected Object handleRequestMessage(Message<?> requestMessage) {
			Message<?> theRequestMessage = requestMessage;
			try {
				theRequestMessage = embeddedHeadersMessageConverter.extractHeaders((Message<byte[]>) requestMessage);
			}
			catch (UnsupportedEncodingException e) {
				logger.error("Could not convert message", e);
			}
			Message<?> result = deserializePayloadIfNecessary(theRequestMessage);
			return result;
		}

		@Override
		public void start() {
		}

		@Override
		public void stop() {
			connector.shutdown();
		}

		@Override
		public boolean isRunning() {
			return true;
		}

	}

	private class SendingHandler extends AbstractMessageHandler {

		private final MessageHandler delegate;

		private final PartitioningMetadata partitioningMetadata;

		private final AtomicInteger roundRobinCount = new AtomicInteger();

		private final String topicName;


		private SendingHandler(MessageHandler delegate, String topicName,
				KafkaPropertiesAccessor properties) {
			this.delegate = delegate;
			this.topicName = topicName;
			this.partitioningMetadata = new PartitioningMetadata(properties);
			this.setBeanFactory(KafkaMessageBus.this.getBeanFactory());
		}

		@Override
		protected void handleMessageInternal(Message<?> message) throws Exception {
			Map<String, Object> additionalHeaders = new HashMap<String, Object>();

			int partition;
			if (partitioningMetadata.isPartitionedModule()) {
				partition = determinePartition(message, partitioningMetadata);
			}
			else {
				// The value will be modulo-ed by numPartitions by Kafka itself
				partition = roundRobin();
			}
			additionalHeaders.put(PARTITION_HEADER, partition);
			additionalHeaders.put("messageKey", partition);
			additionalHeaders.put("topic", topicName);


			@SuppressWarnings("unchecked")
			Message<byte[]> transformed = (Message<byte[]>) serializePayloadIfNecessary(message,
					MimeTypeUtils.APPLICATION_OCTET_STREAM);
			transformed = getMessageBuilderFactory().fromMessage(transformed)
					.copyHeaders(additionalHeaders)
					.build();
			Message<?> messageToSend = embeddedHeadersMessageConverter.embedHeaders(transformed,
					KafkaMessageBus.this.headersToMap);
			Assert.isInstanceOf(byte[].class, messageToSend.getPayload());
			delegate.handleMessage(messageToSend);
		}

		private int roundRobin() {
			int result = roundRobinCount.incrementAndGet();
			if (result == Integer.MAX_VALUE) {
				roundRobinCount.set(0);
			}
			return result;
		}

	}

}
