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

import org.springframework.context.Lifecycle;
import org.springframework.http.MediaType;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.kafka.support.ProducerConfiguration;
import org.springframework.integration.kafka.support.ProducerMetadata;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.integration.bus.AbstractBusPropertiesAccessor;
import org.springframework.xd.dirt.integration.bus.Binding;
import org.springframework.xd.dirt.integration.bus.BusProperties;
import org.springframework.xd.dirt.integration.bus.EmbeddedHeadersMessageConverter;
import org.springframework.xd.dirt.integration.bus.MessageBusSupport;
import org.springframework.xd.dirt.integration.bus.serializer.MultiTypeCodec;

import scala.collection.Seq;

/**
 * A message bus that uses Kafka as the underlying middleware.
 *
 * @author Eric Bottard
 */
public class KafkaMessageBus extends MessageBusSupport {

	private static final String XD_REPLY_CHANNEL = "xdReplyChannel";


	/**
	 * The consumer group to use when achieving point to point semantics (that
	 * consumer group name is static and hence shared by all containers).
	 */
	private static final String POINT_TO_POINT_SEMANTICS_CONSUMER_GROUP = "springXD";

	private class KafkaPropertiesAccessor extends AbstractBusPropertiesAccessor {

		public KafkaPropertiesAccessor(Properties properties) {
			super(properties);
		}

		public int getNumberOfKafkaPartitions() {
			if (new PartitioningMetadata(this).isPartitionedModule()) {
				return getPartitionCount();
			}
			else {
				int downStreamModuleCount = getProperty(NEXT_MODULE_COUNT, 1);
				return downStreamModuleCount == 0 ? numOfKafkaPartitionsForCountEqualsZero : downStreamModuleCount;
			}
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
		public boolean isRunning() {
			return true;
		}

		@Override
		public void start() {
		}

		@Override
		public void stop() {
			connector.shutdown();
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

		private int roundRobin() {
			int result = roundRobinCount.incrementAndGet();
			if (result == Integer.MAX_VALUE) {
				roundRobinCount.set(0);
			}
			return result;
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
					MediaType.APPLICATION_OCTET_STREAM);
			transformed = getMessageBuilderFactory().fromMessage(transformed)
					.copyHeaders(additionalHeaders)
					.build();
			Message<?> messageToSend = embeddedHeadersMessageConverter.embedHeaders(transformed,
					KafkaMessageBus.this.headersToMap);
			Assert.isInstanceOf(byte[].class, messageToSend.getPayload());
			delegate.handleMessage(messageToSend);
		}

	}

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

	private static final Set<Object> SUPPORTED_NAMED_PRODUCER_PROPERTIES = PRODUCER_STANDARD_PROPERTIES;

	/**
	 * Partitioning + kafka producer properties.
	 */
	private static final Set<Object> SUPPORTED_PRODUCER_PROPERTIES = new SetBuilder()
	.addAll(PRODUCER_PARTITIONING_PROPERTIES)
	.addAll(PRODUCER_STANDARD_PROPERTIES)
			.add(BusProperties.DIRECT_BINDING_ALLOWED)
	.build();


	/**
	 * Used when writing directly to ZK. This is what Kafka expects.
	 */
	private final static ZkSerializer utf8Serializer = new ZkSerializer() {

		@Override
		public Object deserialize(byte[] bytes) throws ZkMarshallingError {
			try {
				return new String(bytes, "UTF-8");
			}
			catch (UnsupportedEncodingException e) {
				throw new ZkMarshallingError(e);
			}
		}

		@Override
		public byte[] serialize(Object data) throws ZkMarshallingError {
			try {
				return ((String) data).getBytes("UTF-8");
			}
			catch (UnsupportedEncodingException e) {
				throw new ZkMarshallingError(e);
			}
		}
	};

	/**
	 * Allowed chars are ASCII alphanumerics, '.', '_' and '-'.
	 * '_' is used as escaped char in the form '_xx' where xx is the hexadecimal
	 * value of the byte(s) needed to represent an illegal char in utf8.
	 */
	/*default*/static String escapeTopicName(String original) {
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

	private String brokers;

	private final EmbeddedHeadersMessageConverter embeddedHeadersMessageConverter = new EmbeddedHeadersMessageConverter();


	private ExecutorService executor = Executors.newCachedThreadPool();

	private String[] headersToMap;

	private int replicationFactor = 1;

	private String zkAddress;

	/**
	 * The number of Kafka partitions to use when module count can auto-grow.
	 * Should be bigger than number of containers that will ever exist.
	 */
	private int numOfKafkaPartitionsForCountEqualsZero = 10;

	public KafkaMessageBus(MultiTypeCodec<Object> codec) {
		this("localhost:9092", "localhost:2181", codec);
	}

	public KafkaMessageBus(String brokers, String zkAddress, MultiTypeCodec<Object> codec, String... headersToMap) {
		this.brokers = brokers;
		this.zkAddress = zkAddress;
		if (headersToMap.length > 0) {
			String[] combinedHeadersToMap =
					Arrays.copyOfRange(STANDARD_HEADERS, 0, STANDARD_HEADERS.length + headersToMap.length);
			System.arraycopy(headersToMap, 0, combinedHeadersToMap, STANDARD_HEADERS.length, headersToMap.length);
			this.headersToMap = combinedHeadersToMap;
		}
		else {
			this.headersToMap = STANDARD_HEADERS;
		}
		setCodec(codec);

	}

	/*default*/ConsumerConnector createConsumerConnector(String consumerGroup) {
		Properties props = new Properties();
		props.put("zookeeper.connect", zkAddress);
		props.put("group.id", consumerGroup);
		ConsumerConfig config = new ConsumerConfig(props);
		return Consumer.createJavaConsumerConnector(config);
	}

	@Override
	public void bindConsumer(String name, final MessageChannel moduleInputChannel, Properties properties) {
		createKafkaConsumer(name, moduleInputChannel, properties,
				createConsumerConnector(POINT_TO_POINT_SEMANTICS_CONSUMER_GROUP));
		bindExistingProducerDirectlyIfPossible(name, moduleInputChannel);

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

			ensureTopicCreated(topicName, numPartitions, replicationFactor);


			Properties props = new Properties();
			props.put("metadata.broker.list", brokers);
			props.put("serializer.class", DefaultEncoder.class.getName());
			props.put("key.serializer.class", IntegerEncoderDecoder.class.getName());
			props.put("partitioner.class", DefaultPartitioner.class.getName());
			props.put("request.required.acks", "1");
			ProducerConfig producerConfig = new ProducerConfig(props);


			ProducerMetadata<Integer, byte[]> producerMetadata = new ProducerMetadata<Integer, byte[]>(
					topicName);
			producerMetadata.setValueEncoder(new DefaultEncoder(null));
			producerMetadata.setValueClassType(byte[].class);
			producerMetadata.setKeyEncoder(new IntegerEncoderDecoder(null));
			producerMetadata.setKeyClassType(Integer.class);

			final Producer<Integer, byte[]> producer = new Producer<Integer, byte[]>(producerConfig);
			final ProducerConfiguration<Integer, byte[]> producerConfiguration = new ProducerConfiguration<Integer, byte[]>(
					producerMetadata, producer);

			MessageHandler messageHandler = new AbstractMessageHandler() {

				@Override
				protected void handleMessageInternal(Message<?> message) throws Exception {
					producerConfiguration.send(message);
				}
			};

			Assert.isInstanceOf(SubscribableChannel.class, moduleOutputChannel);
			MessageHandler handler = new SendingHandler(messageHandler, topicName, accessor);
			EventDrivenConsumer consumer = new EventDrivenConsumer((SubscribableChannel) moduleOutputChannel, handler);
			consumer.setBeanFactory(this.getBeanFactory());
			consumer.setBeanName("outbound." + name);
			consumer.afterPropertiesSet();
			Binding producerBinding = Binding.forProducer(name, moduleOutputChannel, consumer, accessor);
			addBinding(producerBinding);
			producerBinding.start();

		}

	}

	@Override
	public void bindPubSubConsumer(String name, MessageChannel inputChannel, Properties properties) {
		// Usage of a different consumer group each time achieves pub-sub
		String group = UUID.randomUUID().toString();
		createKafkaConsumer(name, inputChannel, properties, createConsumerConnector(group));
	}

	@Override
	public void bindPubSubProducer(String name, MessageChannel outputChannel, Properties properties) {
		bindProducer(name, outputChannel, properties);
	}

	@Override
	public void bindReplier(String name, MessageChannel requests, MessageChannel replies, Properties properties) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public void bindRequestor(String name, MessageChannel requests, MessageChannel replies, Properties properties) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
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
		//		int numThreads = accessor.getConcurrency(1);
		int numThreads = 1;
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
				System.out.println(Thread.currentThread());
				ConsumerIterator<Integer, byte[]> it = stream.iterator();
				while (it.hasNext()) {
					byte[] msg = it.next().message();
					bridge.send(MessageBuilder.withPayload(msg).build());
				}
			}

		});
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


}
