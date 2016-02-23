/*
 * Copyright 2014-2016 the original author or authors.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.ZkSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.MediaType;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.codec.Codec;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.DefaultConnectionFactory;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.core.ZookeeperConfiguration;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.integration.kafka.listener.Acknowledgment;
import org.springframework.integration.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.integration.kafka.listener.KafkaNativeOffsetManager;
import org.springframework.integration.kafka.listener.KafkaTopicOffsetManager;
import org.springframework.integration.kafka.listener.OffsetManager;
import org.springframework.integration.kafka.support.KafkaHeaders;
import org.springframework.integration.kafka.support.ProducerConfiguration;
import org.springframework.integration.kafka.support.ProducerFactoryBean;
import org.springframework.integration.kafka.support.ProducerMetadata;
import org.springframework.integration.kafka.support.ZookeeperConnect;
import org.springframework.integration.x.kafka.WindowingOffsetManager;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryOperations;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.integration.bus.AbstractBusPropertiesAccessor;
import org.springframework.xd.dirt.integration.bus.Binding;
import org.springframework.xd.dirt.integration.bus.BusProperties;
import org.springframework.xd.dirt.integration.bus.BusUtils;
import org.springframework.xd.dirt.integration.bus.EmbeddedHeadersMessageConverter;
import org.springframework.xd.dirt.integration.bus.MessageBusSupport;
import org.springframework.xd.dirt.integration.bus.MessageValues;
import org.springframework.xd.dirt.integration.bus.XdHeaders;

import kafka.admin.AdminUtils;
import kafka.admin.AdminUtils$;
import kafka.api.OffsetRequest;
import kafka.serializer.Decoder;
import kafka.serializer.DefaultDecoder;
import kafka.utils.ZkUtils;
import scala.collection.Seq;

/**
 * A message bus that uses Kafka as the underlying middleware. The general implementation mapping between XD concepts
 * and Kafka concepts is as follows:
 * A message bus that uses Kafka as the underlying middleware.
 * The general implementation mapping between XD concepts and Kafka concepts is as follows:
 * <table>
 * <tr>
 * <th>Stream definition</th><th>Kafka topic</th><th>Kafka partitions</th><th>Notes</th>
 * </tr>
 * <tr>
 * <td>foo = "http | log"</td><td>foo.0</td><td>1 partition</td><td>1 producer, 1 consumer</td>
 * </tr>
 * <tr>
 * <td>foo = "http | log", log.count=x</td><td>foo.0</td><td>x partitions</td><td>1 producer, x consumers with static
 * group 'springXD', achieves queue semantics</td>
 * </tr>
 * <tr>
 * <td>foo = "http | log", log.count=x + XD partitioning</td><td>still 1 topic 'foo.0'</td><td>x partitions + use key
 * computed by XD</td><td>1 producer, x consumers with static group 'springXD', achieves queue semantics</td>
 * </tr>
 * <tr>
 * <td>foo = "http | log", log.count=x, concurrency=y</td><td>foo.0</td><td>x*y partitions</td><td>1 producer, x XD
 * consumers, each with y threads</td>
 * </tr>
 * </table>
 * @author Eric Bottard
 * @author Marius Bogoevici
 * @author Ilayaperumal Gopinathan
 * @author David Turanski
 * @author Gary Russell
 * @author Martin Dam
 */
public class KafkaMessageBus extends MessageBusSupport implements DisposableBean {

	public static final ByteArraySerializer BYTE_ARRAY_SERIALIZER = new ByteArraySerializer();

	public static final int METADATA_VERIFICATION_RETRY_ATTEMPTS = 10;

	public static final double METADATA_VERIFICATION_RETRY_BACKOFF_MULTIPLIER = 2;

	public static final int METADATA_VERIFICATION_RETRY_INITIAL_INTERVAL = 100;

	public static final int METADATA_VERIFICATION_MAX_INTERVAL = 1000;

	public static final String FETCH_SIZE = "fetchSize";

	public static final String QUEUE_SIZE = "queueSize";

	public static final String REQUIRED_ACKS = "requiredAcks";

	public static final String COMPRESSION_CODEC = "compressionCodec";

	public static final String AUTO_COMMIT_OFFSET_ENABLED = "autoCommitOffsetEnabled";

	public static final String SYNC_PRODUCER = "syncProducer";

	public static final String SYNC_PRODUCER_TIMEOUT = "syncProducerTimeout";

	private static final String DEFAULT_COMPRESSION_CODEC = "none";

	private static final int DEFAULT_REQUIRED_ACKS = 1;

	private static final boolean DEFAULT_SYNC_PRODUCER = false;

	private static final int DEFAULT_SYNC_PRODUCER_TIMEOUT = 5000;

	private static final boolean DEFAULT_AUTO_COMMIT_OFFSET_ENABLED = true;

	private RetryOperations retryOperations;

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

	/**
	 * The consumer group to use when achieving point to point semantics (that consumer group name is static and hence
	 * shared by all containers).
	 */
	private static final String POINT_TO_POINT_SEMANTICS_CONSUMER_GROUP = "springXD";

	private static final Set<Object> KAFKA_CONSUMER_PROPERTIES = new SetBuilder()
			.add(BusProperties.MIN_PARTITION_COUNT)
			.add(AUTO_COMMIT_OFFSET_ENABLED)
			.add(FETCH_SIZE)
			.add(QUEUE_SIZE)
			.build();

	/**
	 * Basic + concurrency + partitioning.
	 */
	private static final Set<Object> SUPPORTED_CONSUMER_PROPERTIES = new SetBuilder()
			.addAll(CONSUMER_STANDARD_PROPERTIES)
			.addAll(KAFKA_CONSUMER_PROPERTIES)
			.add(BusProperties.PARTITION_INDEX) // Not actually used
			.add(BusProperties.COUNT) // Not actually used
			.add(BusProperties.CONCURRENCY)
			.build();

	private static final Set<Object> SUPPORTED_PUBSUB_CONSUMER_PROPERTIES = new SetBuilder()
			.addAll(SUPPORTED_CONSUMER_PROPERTIES)
			.addAll(KAFKA_CONSUMER_PROPERTIES)
			.add(BusProperties.DURABLE)
			.build();

	/**
	 * Basic + concurrency.
	 */
	private static final Set<Object> SUPPORTED_NAMED_CONSUMER_PROPERTIES = new SetBuilder()
			.addAll(CONSUMER_STANDARD_PROPERTIES)
			.addAll(KAFKA_CONSUMER_PROPERTIES)
			.add(BusProperties.CONCURRENCY)
			.build();

	private static final Set<Object> SUPPORTED_NAMED_PUBSUB_CONSUMER_PROPERTIES = new SetBuilder()
			.addAll(SUPPORTED_NAMED_CONSUMER_PROPERTIES)
			.add(BusProperties.DURABLE)
			.build();

	private static final Set<Object> KAFKA_PRODUCER_PROPERTIES = new SetBuilder()
			.add(BusProperties.MIN_PARTITION_COUNT)
			.add(SYNC_PRODUCER)
			.add(SYNC_PRODUCER_TIMEOUT)
			.build();

	private static final Set<Object> SUPPORTED_NAMED_PRODUCER_PROPERTIES = new SetBuilder()
			.addAll(PRODUCER_STANDARD_PROPERTIES)
			.addAll(PRODUCER_BATCHING_BASIC_PROPERTIES)
			.addAll(PRODUCER_COMPRESSION_PROPERTIES)
			.addAll(KAFKA_PRODUCER_PROPERTIES)
			.addAll(PRODUCER_PARTITIONING_PROPERTIES)
			.build();


	/**
	 * Partitioning + kafka producer properties.
	 */
	private static final Set<Object> SUPPORTED_PRODUCER_PROPERTIES = new SetBuilder()
			.addAll(PRODUCER_PARTITIONING_PROPERTIES)
			.addAll(PRODUCER_STANDARD_PROPERTIES)
			.add(BusProperties.DIRECT_BINDING_ALLOWED)
			.addAll(KAFKA_PRODUCER_PROPERTIES)
			.addAll(PRODUCER_BATCHING_BASIC_PROPERTIES)
			.addAll(PRODUCER_COMPRESSION_PROPERTIES)
			.build();

	private final EmbeddedHeadersMessageConverter embeddedHeadersMessageConverter = new
			EmbeddedHeadersMessageConverter();

	private final ZookeeperConnect zookeeperConnect;

	private String brokers;

	private String[] headersToMap;

	private String zkAddress;

	// -------- Default values for properties -------
	private int defaultReplicationFactor = 1;

	private String defaultCompressionCodec = DEFAULT_COMPRESSION_CODEC;

	private int defaultRequiredAcks = DEFAULT_REQUIRED_ACKS;

	private boolean defaultSyncProducer = DEFAULT_SYNC_PRODUCER;

	private int defaultSyncProducerTimeout = DEFAULT_SYNC_PRODUCER_TIMEOUT;

	private int defaultQueueSize = 1024;

	private int defaultMaxWait = 100;

	private int defaultFetchSize = 1024 * 1024;

	private int defaultMinPartitionCount = 1;

	private ConnectionFactory connectionFactory;

	private String offsetStoreTopic = "SpringXdOffsets";

	// auto commit property

	private boolean defaultAutoCommitOffsetEnabled = DEFAULT_AUTO_COMMIT_OFFSET_ENABLED;

	private boolean defaultDurableSubscription = false;

	private int socketBufferSize = 2097152;

	private int offsetStoreSegmentSize = 250 * 1024 * 1024;

	private int offsetStoreRetentionTime = 60000;

	private int offsetStoreRequiredAcks = 1;

	private int offsetStoreMaxFetchSize = 1048576;

	private int offsetStoreBatchBytes = 200;

	private int offsetStoreBatchTime = 1000;

	private int offsetUpdateTimeWindow = 10000;

	private int offsetUpdateCount = 0;

	private int offsetUpdateShutdownTimeout = 2000;

	private Mode mode = Mode.embeddedHeaders;

	private OffsetManagement offsetManagement = OffsetManagement.kafkaTopic;

	private ZkClient zkClient;

	public KafkaMessageBus(ZookeeperConnect zookeeperConnect, String brokers, String zkAddress,
			Codec codec, String... headersToMap) {
		this.zookeeperConnect = zookeeperConnect;
		this.brokers = brokers;
		this.zkAddress = zkAddress;
		setCodec(codec);
		if (headersToMap.length > 0) {
			String[] combinedHeadersToMap =
					Arrays.copyOfRange(XdHeaders.STANDARD_HEADERS, 0, XdHeaders.STANDARD_HEADERS.length + headersToMap
							.length);
			System.arraycopy(headersToMap, 0, combinedHeadersToMap, XdHeaders.STANDARD_HEADERS.length, headersToMap
					.length);
			this.headersToMap = combinedHeadersToMap;
		}
		else {
			this.headersToMap = XdHeaders.STANDARD_HEADERS;
		}

	}

	public void setOffsetStoreTopic(String offsetStoreTopic) {
		this.offsetStoreTopic = offsetStoreTopic;
	}

	public void setOffsetStoreSegmentSize(int offsetStoreSegmentSize) {
		this.offsetStoreSegmentSize = offsetStoreSegmentSize;
	}

	public void setOffsetStoreRetentionTime(int offsetStoreRetentionTime) {
		this.offsetStoreRetentionTime = offsetStoreRetentionTime;
	}

	public void setSocketBufferSize(int socketBufferSize) {
		this.socketBufferSize = socketBufferSize;
	}

	public void setOffsetStoreRequiredAcks(int offsetStoreRequiredAcks) {
		this.offsetStoreRequiredAcks = offsetStoreRequiredAcks;
	}

	public void setOffsetStoreMaxFetchSize(int offsetStoreMaxFetchSize) {
		this.offsetStoreMaxFetchSize = offsetStoreMaxFetchSize;
	}


	public void setOffsetUpdateTimeWindow(int offsetUpdateTimeWindow) {
		this.offsetUpdateTimeWindow = offsetUpdateTimeWindow;
	}

	public void setOffsetUpdateCount(int offsetUpdateCount) {
		this.offsetUpdateCount = offsetUpdateCount;
	}

	public void setOffsetUpdateShutdownTimeout(int offsetUpdateShutdownTimeout) {
		this.offsetUpdateShutdownTimeout = offsetUpdateShutdownTimeout;
	}

	public void setOffsetStoreBatchBytes(int offsetStoreBatchBytes) {
		this.offsetStoreBatchBytes = offsetStoreBatchBytes;
	}

	public void setOffsetStoreBatchTime(int offsetStoreBatchTime) {
		this.offsetStoreBatchTime = offsetStoreBatchTime;
	}

	public ConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}

	/**
	 * Retry configuration for operations such as validating topic creation
	 * @param retryOperations the retry configuration
	 */
	public void setRetryOperations(RetryOperations retryOperations) {
		this.retryOperations = retryOperations;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// we instantiate the connection factory here due to https://jira.spring.io/browse/XD-2647
		ZookeeperConfiguration configuration = new ZookeeperConfiguration(this.zookeeperConnect);
		zkClient = new ZkClient(zkAddress, 10000, 10000, utf8Serializer);
		zkClient.waitUntilConnected(10000, TimeUnit.MILLISECONDS);
		configuration.setBufferSize(socketBufferSize);
		configuration.setMaxWait(defaultMaxWait);
		DefaultConnectionFactory defaultConnectionFactory =
				new DefaultConnectionFactory(configuration);
		defaultConnectionFactory.afterPropertiesSet();
		this.connectionFactory = defaultConnectionFactory;
		if (retryOperations == null) {
			RetryTemplate retryTemplate = new RetryTemplate();

			SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
			simpleRetryPolicy.setMaxAttempts(METADATA_VERIFICATION_RETRY_ATTEMPTS);
			retryTemplate.setRetryPolicy(simpleRetryPolicy);

			ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
			backOffPolicy.setInitialInterval(METADATA_VERIFICATION_RETRY_INITIAL_INTERVAL);
			backOffPolicy.setMultiplier(METADATA_VERIFICATION_RETRY_BACKOFF_MULTIPLIER);
			backOffPolicy.setMaxInterval(METADATA_VERIFICATION_MAX_INTERVAL);
			retryTemplate.setBackOffPolicy(backOffPolicy);
			retryOperations = retryTemplate;
		}
	}

	@Override
	public void destroy() throws Exception {
		zkClient.close();
	}

	@Override
	public boolean isCapable(Capability capability) {
		switch (capability) {
		case DURABLE_PUBSUB:
		case NATIVE_PARTITIONING:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Allowed chars are ASCII alphanumerics, '.', '_' and '-'. '_' is used as escaped char in the form '_xx' where xx
	 * is the hexadecimal value of the byte(s) needed to represent an illegal char in utf8.
	 */
	/*default*/
	public static String escapeTopicName(String original) {
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

	/**
	 * Set the default auto commit enabled property; This is used to commit the offset either automatically or
	 * manually.
	 * @param defaultAutoCommitOffsetEnabled
	 */
	public void setDefaultAutoCommitOffsetEnabled(boolean defaultAutoCommitOffsetEnabled) {
		this.defaultAutoCommitOffsetEnabled = defaultAutoCommitOffsetEnabled;
	}

	public void setDefaultQueueSize(int defaultQueueSize) {
		this.defaultQueueSize = defaultQueueSize;
	}

	public void setDefaultFetchSize(int defaultFetchSize) {
		this.defaultFetchSize = defaultFetchSize;
	}

	public void setDefaultMinPartitionCount(int defaultMinPartitionCount) {
		this.defaultMinPartitionCount = defaultMinPartitionCount;
	}

	public void setDefaultMaxWait(int defaultMaxWait) {
		this.defaultMaxWait = defaultMaxWait;
	}

	public void setDefaultSyncProducer(boolean syncProducer) {
		this.defaultSyncProducer = syncProducer;
	}

	public void setDefaultSyncProducerTimeout(int timeout) {
		this.defaultSyncProducerTimeout = timeout;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	/**
	 * Set the {@link OffsetManagement} to use. Default:
	 * {@link OffsetManagement#kafkaTopic}.
	 * @param offsetManagement the offsetManagement.
	 */
	public void setOffsetManagement(OffsetManagement offsetManagement) {
		this.offsetManagement = offsetManagement;
	}

	@Override
	public void bindConsumer(String name, final MessageChannel moduleInputChannel, Properties properties) {
		// Point-to-point consumers reset at the earliest time, which allows them to catch up with all messages
		createKafkaConsumer(name, moduleInputChannel, properties, false, OffsetRequest.EarliestTime());
		bindExistingProducerDirectlyIfPossible(name, moduleInputChannel);
	}

	@Override
	public void bindPubSubConsumer(String name, MessageChannel inputChannel, Properties properties) {
		createKafkaConsumer(name, inputChannel, properties, true, OffsetRequest.LatestTime());
	}

	@Override
	public void bindProducer(final String name, MessageChannel moduleOutputChannel, Properties properties) {

		Assert.isInstanceOf(SubscribableChannel.class, moduleOutputChannel);
		KafkaPropertiesAccessor accessor = new KafkaPropertiesAccessor(properties);
		if (name.startsWith(P2P_NAMED_CHANNEL_TYPE_PREFIX) || name.startsWith(PUBSUB_NAMED_CHANNEL_TYPE_PREFIX)) {
			validateProducerProperties(name, properties, SUPPORTED_NAMED_PRODUCER_PROPERTIES);
		}
		else {
			validateProducerProperties(name, properties, SUPPORTED_PRODUCER_PROPERTIES);
		}

		Collection<Partition> partitions;
		if (!bindNewProducerDirectlyIfPossible(name, (SubscribableChannel) moduleOutputChannel, accessor)) {
			if (logger.isInfoEnabled()) {
				logger.info("Using Kafka topic for outbound: " + name);
			}

			final String topicName = escapeTopicName(name);
			int defaultPartitionCount = accessor.getNumberOfKafkaPartitionsForProducer();

			partitions = initializePartitions(name, topicName, defaultPartitionCount);

			ProducerMetadata<byte[], byte[]> producerMetadata = new ProducerMetadata<>(topicName, byte[].class,
					byte[].class, BYTE_ARRAY_SERIALIZER, BYTE_ARRAY_SERIALIZER);
			producerMetadata.setCompressionType(ProducerMetadata.CompressionType
					.valueOf(accessor.getCompressionCodec(this.defaultCompressionCodec)));
			producerMetadata.setBatchBytes(accessor.getBatchSize(this.defaultBatchSize));
			producerMetadata.setSync(accessor.getSyncProducer(this.defaultSyncProducer));
			producerMetadata.setSendTimeout(accessor.getSyncProducerTimeout(this.defaultSyncProducerTimeout));
			Properties additionalProps = new Properties();
			additionalProps.put(ProducerConfig.ACKS_CONFIG,
					String.valueOf(accessor.getRequiredAcks(this.defaultRequiredAcks)));
			additionalProps.put(ProducerConfig.LINGER_MS_CONFIG,
					String.valueOf(accessor.getBatchTimeout(this.defaultBatchTimeout)));
			ProducerFactoryBean<byte[], byte[]> producerFB = new ProducerFactoryBean<>(producerMetadata, brokers,
					additionalProps);

			try {
				final ProducerConfiguration<byte[], byte[]> producerConfiguration = new ProducerConfiguration<>(
						producerMetadata, producerFB.getObject());
				MessageHandler handler = new SendingHandler(topicName, accessor, partitions.size(),
						producerConfiguration);
				EventDrivenConsumer consumer = new EventDrivenConsumer((SubscribableChannel) moduleOutputChannel,
						handler);
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

	private Collection<Partition> initializePartitions(String name, String topicName, int defaultPartitionCount) {
		Collection<Partition> partitions;
		try {
			// existing topics for named channels or taps should never be repartitioned
			if ((isNamedChannel(name) || isTap(name)) && topicExists(topicName)) {
				partitions = getPartitions(topicName);
			}
			else {
				partitions = ensureTopicCreated(topicName, defaultPartitionCount, defaultReplicationFactor, true);
			}
		}
		catch (Exception exception) {
			throw new RuntimeException("Cannot initialize message bus:", exception);
		}
		return partitions;
	}

	@Override
	public void bindPubSubProducer(String name, MessageChannel outputChannel, Properties properties) {
		bindProducer(name, outputChannel, properties);
	}

	@Override
	public void bindRequestor(String name, MessageChannel requests, MessageChannel replies, Properties properties) {
		throw new UnsupportedOperationException("requestor binding is not supported by this bus");
	}

	@Override
	public void bindReplier(String name, MessageChannel requests, MessageChannel replies, Properties properties) {
		throw new UnsupportedOperationException("replier binding is not supported by this bus");
	}

	/**
	 * Creates a Kafka topic if needed, or try to increase its partition count to the desired number.
	 */
	private Collection<Partition> ensureTopicCreated(final String topicName, final int numPartitions,
			int replicationFactor, final boolean createOrResize) {
		// The following is basically copy/paste from AdminUtils.createTopic() with
		// createOrUpdateTopicPartitionAssignmentPathInZK(..., update=true)
		if (createOrResize) {
			final Properties topicConfig = new Properties();
			Seq<Object> brokerList = ZkUtils.getSortedBrokerList(zkClient);
			final scala.collection.Map<Object, Seq<Object>> replicaAssignment =
					AdminUtils.assignReplicasToBrokers(brokerList, numPartitions, replicationFactor, -1, -1);
			retryOperations.execute(new RetryCallback<Object, RuntimeException>() {
				@Override public Object doWithRetry(RetryContext context) throws RuntimeException {
					AdminUtils.createOrUpdateTopicPartitionAssignmentPathInZK(zkClient, topicName, replicaAssignment,
							topicConfig, true);
					return null;
				}
			});
		}
		return getPartitions(topicName, numPartitions);
	}

	/**
	 * Fetches partition data, throwing an exception if the expected number is not met after a number of
	 * subsequent retries
	 * @param topicName topic
	 * @param expectedNumber expected number of partitions. If less or equal to 0, check is suppressed.
	 * @return the partitions
	 */
	private Collection<Partition> getPartitions(final String topicName, final int expectedNumber) {
		try {
			Collection<Partition> partitions =
					retryOperations.execute(new RetryCallback<Collection<Partition>, Exception>() {
						@Override public Collection<Partition> doWithRetry(RetryContext context) throws Exception {
							connectionFactory.refreshMetadata(Collections.singleton(topicName));
							Collection<Partition> partitions = connectionFactory.getPartitions(topicName);
							if (expectedNumber > 0 && partitions.size() < expectedNumber) {
								throw new IllegalStateException(
										"The number of expected partitions was: " + expectedNumber + ", but "
												+ partitions.size() + " have been found instead");
							}
							connectionFactory.getLeaders(partitions);
							return partitions;
						}
					});
			return partitions;
		}
		catch (Exception e) {
			logger.error("Cannot initialize MessageBus", e);
			throw new RuntimeException("Cannot initialize message bus:", e);
		}
	}

	private Collection<Partition> getPartitions(final String topicName) {
		return getPartitions(topicName, 0);
	}

	private void createKafkaConsumer(String name, final MessageChannel moduleInputChannel, Properties properties,
			boolean pubSub, long referencePoint) {

		// we remove the group qualifier
		String unqualifiedName = pubSub ? BusUtils.removeGroupFromPubSub(name) : name;

		if (pubSub) {
			if (isNamedChannel(unqualifiedName)) {
				validateConsumerProperties(name, properties, SUPPORTED_NAMED_PUBSUB_CONSUMER_PROPERTIES);
			}
			else {
				validateConsumerProperties(name, properties, SUPPORTED_PUBSUB_CONSUMER_PROPERTIES);
			}
		}
		else {
			if (isNamedChannel(unqualifiedName)) {
				validateConsumerProperties(name, properties, SUPPORTED_NAMED_CONSUMER_PROPERTIES);
			}
			else {
				validateConsumerProperties(name, properties, SUPPORTED_CONSUMER_PROPERTIES);
			}
		}
		KafkaPropertiesAccessor accessor = new KafkaPropertiesAccessor(properties);

		int maxConcurrency = accessor.getConcurrency(defaultConcurrency);



		String listenedTopicName = escapeTopicName(unqualifiedName);

		int partitionCountFromConfiguration = accessor.getNumberOfKafkaPartitionsForConsumer();
		Collection<Partition> partitions = initializePartitions(
				unqualifiedName, listenedTopicName, partitionCountFromConfiguration);

		Decoder<byte[]> valueDecoder = new DefaultDecoder(null);
		Decoder<byte[]> keyDecoder = new DefaultDecoder(null);

		Collection<Partition> listenedPartitions;

		int moduleCount = accessor.getCount();

		if (moduleCount == 1) {
			listenedPartitions = partitions;
		}
		else {
			listenedPartitions = new ArrayList<Partition>();
			for (Partition partition : partitions) {
				// divide partitions across modules
				if (accessor.getPartitionIndex() != -1) {
					if ((partition.getId() % moduleCount) == accessor.getPartitionIndex()) {
						listenedPartitions.add(partition);
					}
				}
				else {
					int moduleSequence = accessor.getSequence();
					if (moduleCount == 0) {
						throw new IllegalArgumentException("The Kafka transport does not support 0-count modules");
					}
					else {
						// sequence numbers are zero-based
						if ((partition.getId() % moduleCount) == (moduleSequence - 1)) {
							listenedPartitions.add(partition);
						}
					}
				}
			}
		}

		ReceivingHandler rh = new ReceivingHandler();
		rh.setOutputChannel(moduleInputChannel);

		final FixedSubscriberChannel bridge = new FixedSubscriberChannel(rh);
		bridge.setBeanName("bridge." + name);

		String group = pubSub ? BusUtils.getGroupFromPubSub(name) : POINT_TO_POINT_SEMANTICS_CONSUMER_GROUP;

		boolean resetOffsets = pubSub && !accessor.isDurable(defaultDurableSubscription);

		final KafkaMessageListenerContainer messageListenerContainer =
				createMessageListenerContainer(accessor,
						group, maxConcurrency, listenedPartitions,
						referencePoint, resetOffsets);

		final KafkaMessageDrivenChannelAdapter kafkaMessageDrivenChannelAdapter =
				new KafkaMessageDrivenChannelAdapter(messageListenerContainer);
		kafkaMessageDrivenChannelAdapter.setBeanFactory(this.getBeanFactory());
		kafkaMessageDrivenChannelAdapter.setKeyDecoder(keyDecoder);
		kafkaMessageDrivenChannelAdapter.setPayloadDecoder(valueDecoder);
		kafkaMessageDrivenChannelAdapter.setOutputChannel(bridge);
		kafkaMessageDrivenChannelAdapter.setAutoCommitOffset(
				accessor.getAutoCommitOffsetEnabled(this.defaultAutoCommitOffsetEnabled));
		kafkaMessageDrivenChannelAdapter.afterPropertiesSet();
		kafkaMessageDrivenChannelAdapter.start();


		EventDrivenConsumer edc = new KafkaConsumerBusAdapter(bridge, rh, kafkaMessageDrivenChannelAdapter, messageListenerContainer);
		edc.setBeanName("inbound." + name);

		Binding consumerBinding = Binding.forConsumer(name, edc, moduleInputChannel, accessor);
		addBinding(consumerBinding);
		consumerBinding.start();

	}

	private boolean topicExists(String tappedTopicName) {
		return AdminUtils$.MODULE$.topicExists(zkClient, tappedTopicName);
	}

	private boolean isTap(String name) {
		return name.startsWith(TAP_TYPE_PREFIX);
	}

	public KafkaMessageListenerContainer createMessageListenerContainer(Properties properties, String group,
			int maxConcurrency, String topic, long referencePoint, boolean resetOffsets) {
		return createMessageListenerContainer(new KafkaPropertiesAccessor(properties), group, maxConcurrency, topic,
				null, referencePoint, resetOffsets);
	}

	private KafkaMessageListenerContainer createMessageListenerContainer(KafkaPropertiesAccessor accessor,
			String group, int maxConcurrency, Collection<Partition> listenedPartitions, long referencePoint,
			boolean resetOffsets) {
		return createMessageListenerContainer(accessor, group, maxConcurrency, null, listenedPartitions,
				referencePoint, resetOffsets);
	}

	private KafkaMessageListenerContainer createMessageListenerContainer(KafkaPropertiesAccessor accessor,
			String group, int maxConcurrency, String topic, Collection<Partition> listenedPartitions,
			long referencePoint, boolean resetOffsets) {
		Assert.isTrue(StringUtils.hasText(topic) ^ !CollectionUtils.isEmpty(listenedPartitions),
				"Exactly one of topic or a list of listened partitions must be provided");
		KafkaMessageListenerContainer messageListenerContainer;
		if (topic != null) {
			messageListenerContainer = new KafkaMessageListenerContainer(connectionFactory, topic);
		}
		else {
			messageListenerContainer = new KafkaMessageListenerContainer(connectionFactory,
					listenedPartitions.toArray(new Partition[listenedPartitions.size()]));
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Listening to topic " + topic);
		}
		// if we have less target partitions than target concurrency, adjust accordingly
		messageListenerContainer.setConcurrency(Math.min(maxConcurrency, listenedPartitions.size()));
		OffsetManager offsetManager = createOffsetManager(group, referencePoint);
		if (resetOffsets) {
			offsetManager.resetOffsets(listenedPartitions);
		}
		messageListenerContainer.setOffsetManager(offsetManager);
		int queueSize = accessor.getProperty(QUEUE_SIZE, defaultQueueSize);
		Assert.isTrue(queueSize > 0 && Integer.bitCount(queueSize) == 1, "must be a power of 2");
		messageListenerContainer.setQueueSize(queueSize);
		messageListenerContainer.setMaxFetch(accessor.getProperty(FETCH_SIZE, defaultFetchSize));
		return messageListenerContainer;
	}

	private OffsetManager createOffsetManager(String group, long referencePoint) {
		try {
			OffsetManager delegateOffsetManager;
			if (OffsetManagement.kafkaNative.equals(offsetManagement)) {
				KafkaNativeOffsetManager kafkaOffsetManager = new KafkaNativeOffsetManager(connectionFactory,
						zookeeperConnect, Collections.<Partition, Long>emptyMap());
				kafkaOffsetManager.setConsumerId(group);
				kafkaOffsetManager.setReferenceTimestamp(referencePoint);
				kafkaOffsetManager.afterPropertiesSet();
				delegateOffsetManager = kafkaOffsetManager;
			}
			else {
				KafkaTopicOffsetManager kafkaOffsetManager = new KafkaTopicOffsetManager(zookeeperConnect,
						offsetStoreTopic, Collections.<Partition, Long>emptyMap());
				kafkaOffsetManager.setConsumerId(group);
				kafkaOffsetManager.setReferenceTimestamp(referencePoint);
				kafkaOffsetManager.setSegmentSize(offsetStoreSegmentSize);
				kafkaOffsetManager.setRetentionTime(offsetStoreRetentionTime);
				kafkaOffsetManager.setRequiredAcks(offsetStoreRequiredAcks);
				kafkaOffsetManager.setMaxSize(offsetStoreMaxFetchSize);
				kafkaOffsetManager.setBatchBytes(offsetStoreBatchBytes);
				kafkaOffsetManager.setMaxQueueBufferingTime(offsetStoreBatchTime);
				kafkaOffsetManager.afterPropertiesSet();
				delegateOffsetManager = kafkaOffsetManager;
			}
			WindowingOffsetManager windowingOffsetManager = new WindowingOffsetManager(delegateOffsetManager);
			windowingOffsetManager.setTimespan(offsetUpdateTimeWindow);
			windowingOffsetManager.setCount(offsetUpdateCount);
			windowingOffsetManager.setShutdownTimeout(offsetUpdateShutdownTimeout);

			windowingOffsetManager.afterPropertiesSet();
			return windowingOffsetManager;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void doManualAck(LinkedList<MessageHeaders> messageHeadersList) {
		Iterator<MessageHeaders> iterator = messageHeadersList.iterator();
		while (iterator.hasNext()) {
			MessageHeaders headers = iterator.next();
			Acknowledgment acknowledgment = (Acknowledgment) headers.get(KafkaHeaders.ACKNOWLEDGMENT);
			Assert.notNull(acknowledgment, "Acknowledgement shouldn't be null when acknowledging kafka message " +
					"manually.");
			acknowledgment.acknowledge();
		}
	}

	private static class KafkaConsumerBusAdapter extends EventDrivenConsumer {

		private final KafkaMessageDrivenChannelAdapter kafkaMessageDrivenChannelAdapter;

		private final KafkaMessageListenerContainer messageListenerContainer;

		public KafkaConsumerBusAdapter(FixedSubscriberChannel bridge, ReceivingHandler rh, KafkaMessageDrivenChannelAdapter kafkaMessageDrivenChannelAdapter, KafkaMessageListenerContainer messageListenerContainer) {
			super(bridge, rh);
			this.kafkaMessageDrivenChannelAdapter = kafkaMessageDrivenChannelAdapter;
			this.messageListenerContainer = messageListenerContainer;
		}

		@Override
		protected void doStop() {
			// stop the offset manager and the channel adapter before unbinding
			// this means that the upstream channel adapter has a chance to stop
			kafkaMessageDrivenChannelAdapter.stop();
			if (messageListenerContainer.getOffsetManager() instanceof DisposableBean) {
				try {
					((DisposableBean) messageListenerContainer.getOffsetManager()).destroy();
				}
				catch (Exception e) {
					logger.error("Error while closing the offset manager", e);
				}
			}
			super.doStop();
		}
	}

	private class KafkaPropertiesAccessor extends AbstractBusPropertiesAccessor {

		public KafkaPropertiesAccessor(Properties properties) {
			super(properties);
		}

		public int getNumberOfKafkaPartitionsForProducer() {
			int nextModuleCount = getNextModuleCount();
			if (nextModuleCount == 0) {
				throw new IllegalArgumentException("Module count cannot be zero");
			}
			int nextModuleConcurrency = getProperty(NEXT_MODULE_CONCURRENCY, defaultConcurrency);
			int minKafkaPartitions = getMinPartitionCount(defaultMinPartitionCount);
			return Math.max(minKafkaPartitions, nextModuleCount * nextModuleConcurrency);
		}

		public int getNumberOfKafkaPartitionsForConsumer() {
			int concurrency = getConcurrency(defaultConcurrency);
			int minKafkaPartitions = getMinPartitionCount(defaultMinPartitionCount);
			int moduleCount = getCount();
			if (moduleCount == 0) {
				throw new IllegalArgumentException("Module count cannot be zero");
			}
			return Math.max(minKafkaPartitions, moduleCount * concurrency);
		}

		public String getCompressionCodec(String defaultValue) {
			return getProperty(COMPRESSION_CODEC, defaultValue);
		}

		public int getRequiredAcks(int defaultRequiredAcks) {
			return getProperty(REQUIRED_ACKS, defaultRequiredAcks);
		}

		public boolean getAutoCommitOffsetEnabled(boolean defaultAutoOffsetCommitEnabled) {
			return getProperty(AUTO_COMMIT_OFFSET_ENABLED, defaultAutoOffsetCommitEnabled);
		}

		public int getMinPartitionCount(int defaultPartitionCount) {
			return getProperty(MIN_PARTITION_COUNT, defaultPartitionCount);
		}

		public boolean getSyncProducer(boolean defaultSyncProducer) {
			return getProperty(SYNC_PRODUCER, defaultSyncProducer);
		}

		public int getSyncProducerTimeout(int defaultSyncProducerTimeout) {
			return getProperty(SYNC_PRODUCER_TIMEOUT, defaultSyncProducerTimeout);
		}
	}

	private class ReceivingHandler extends AbstractReplyProducingMessageHandler {

		public ReceivingHandler() {
			this.setBeanFactory(KafkaMessageBus.this.getBeanFactory());
		}

		@Override
		@SuppressWarnings("unchecked")
		protected Object handleRequestMessage(Message<?> requestMessage) {
			if (Mode.embeddedHeaders.equals(mode)) {
				MessageValues messageValues;
				try {
					messageValues = embeddedHeadersMessageConverter.extractHeaders((Message<byte[]>) requestMessage,
							true);
				}
				catch (Exception e) {
					logger.error(EmbeddedHeadersMessageConverter.decodeExceptionMessage(requestMessage), e);
					messageValues = new MessageValues(requestMessage);
				}
				messageValues = deserializePayloadIfNecessary(messageValues);
				return MessageBuilder.createMessage(messageValues.getPayload(), new KafkaBusMessageHeaders(
						messageValues));
			}
			else {
				return requestMessage;
			}
		}

		@SuppressWarnings("serial")
		private final class KafkaBusMessageHeaders extends MessageHeaders {

			KafkaBusMessageHeaders(Map<String, Object> headers) {
				super(headers, MessageHeaders.ID_VALUE_NONE, -1L);
			}
		}


		@Override
		protected boolean shouldCopyRequestHeaders() {
			// prevent the message from being copied again in superclass
			return false;
		}
	}

	private class SendingHandler extends AbstractMessageHandler {

		private final PartitioningMetadata partitioningMetadata;

		private final AtomicInteger roundRobinCount = new AtomicInteger(-1);

		private final String topicName;

		private final int numberOfKafkaPartitions;

		private final ProducerConfiguration<byte[], byte[]> producerConfiguration;


		private SendingHandler(String topicName, KafkaPropertiesAccessor properties, int numberOfPartitions,
				ProducerConfiguration<byte[], byte[]> producerConfiguration) {
			this.topicName = topicName;
			this.numberOfKafkaPartitions = numberOfPartitions;
			this.partitioningMetadata = new PartitioningMetadata(properties, numberOfPartitions);
			this.setBeanFactory(KafkaMessageBus.this.getBeanFactory());
			this.producerConfiguration = producerConfiguration;
		}

		@Override
		protected void handleMessageInternal(Message<?> message) throws Exception {
			int targetPartition;
			if (partitioningMetadata.isPartitionedModule()) {
				targetPartition = determinePartition(message, partitioningMetadata);
			}
			else {
				targetPartition = roundRobin() % numberOfKafkaPartitions;
			}

			if (Mode.embeddedHeaders.equals(mode)) {
				MessageValues transformed = serializePayloadIfNecessary(message);
				byte[] messageToSend = embeddedHeadersMessageConverter.embedHeaders(transformed,
						KafkaMessageBus.this.headersToMap);
				producerConfiguration.send(topicName, targetPartition, null, messageToSend);
			}
			else if (Mode.raw.equals(mode)) {
				Object contentType = message.getHeaders().get(MessageHeaders.CONTENT_TYPE);
				if (contentType != null
						&& !contentType.equals(MediaType.APPLICATION_OCTET_STREAM_VALUE)) {
					logger.error("Raw mode supports only " + MediaType.APPLICATION_OCTET_STREAM_VALUE + " content type"
							+ message.getPayload().getClass());
				}
				if (message.getPayload() instanceof byte[]) {
					producerConfiguration.send(topicName, targetPartition, null, (byte[]) message.getPayload());
				}
				else {
					logger.error("Raw mode supports only byte[] payloads but value sent was of type "
							+ message.getPayload().getClass());
				}
			}
		}

		private int roundRobin() {
			int result = roundRobinCount.incrementAndGet();
			if (result == Integer.MAX_VALUE) {
				roundRobinCount.set(0);
			}
			return result;
		}

	}

	public enum Mode {
		raw,
		embeddedHeaders
	}

	public enum OffsetManagement {
		kafkaTopic,
		kafkaNative
	}

}
