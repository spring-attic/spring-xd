/*
 * Copyright 2015 the original author or authors.
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


package org.springframework.integration.x.kafka;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import kafka.admin.AdminUtils$;
import kafka.api.OffsetRequest;
import kafka.common.ErrorMapping$;
import kafka.common.TopicExistsException;
import kafka.javaapi.producer.Producer;
import kafka.producer.DefaultPartitioner;
import kafka.producer.KeyedMessage;
import kafka.utils.Utils$;
import kafka.utils.ZKStringSerializer$;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkInterruptedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.kafka.core.BrokerAddress;
import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.FetchRequest;
import org.springframework.integration.kafka.core.KafkaMessage;
import org.springframework.integration.kafka.core.KafkaMessageBatch;
import org.springframework.integration.kafka.core.KafkaTemplate;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.core.Result;
import org.springframework.integration.kafka.core.TopicNotFoundException;
import org.springframework.integration.kafka.serializer.common.StringDecoder;
import org.springframework.integration.kafka.serializer.common.StringEncoder;
import org.springframework.integration.kafka.support.ProducerFactoryBean;
import org.springframework.integration.kafka.support.ProducerMetadata;
import org.springframework.integration.kafka.support.ZookeeperConnect;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Implementation of a {@link MetadataStore} that uses a Kafka topic as the underlying support.
 * For its proper functioning, the Kafka server(s) <emphasis>must</emphasis> set
 * {@code log.cleaner.enable=true}.
 *
 * @author Marius Bogoevici
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class KafkaTopicMetadataStore implements InitializingBean, MetadataStore, Closeable {

	private static final Log log = LogFactory.getLog(KafkaTopicMetadataStore.class);

	public static final StringEncoder ENCODER = new StringEncoder();

	public static final StringDecoder DECODER = new StringDecoder();

	private final ZookeeperConnect zookeeperConnect;

	private final ConnectionFactory connectionFactory;

	private final String topic;

	private final KafkaTemplate kafkaTemplate;

	private int maxSize = 10 * 1024;

	private final ConcurrentMap<String, String> data = new ConcurrentHashMap<String, String>();

	private String compressionCodec = "default";

	private Producer<String, String> producer;

	private int maxQueueBufferingTime = 1000;

	private int segmentSize = 25 * 1024;

	private int retentionTime = 60000;

	private int replicationFactor;

	private int maxBatchSize = 200;

	public KafkaTopicMetadataStore(ZookeeperConnect zookeeperConnect, ConnectionFactory connectionFactory, String topic) {
		this.zookeeperConnect = zookeeperConnect;
		this.connectionFactory = connectionFactory;
		this.kafkaTemplate = new KafkaTemplate(connectionFactory);
		this.topic = topic;
	}

	/**
	 * Sets the maximum size of a fetch request, allowing to tune the initialization process.
	 *
	 * @param maxSize the maximum amount of data to be brought on a fetch
	 */
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	/**
	 * The compression codec for writing to the offset topic
	 *
	 * @param compressionCodec the compression codec
	 */
	public void setCompressionCodec(String compressionCodec) {
		this.compressionCodec = compressionCodec;
	}

	/**
	 * For how long will producers buffer data before writing to the topic
	 *
	 * @param maxQueueBufferingTime
	 */
	public void setMaxQueueBufferingTime(int maxQueueBufferingTime) {
		this.maxQueueBufferingTime = maxQueueBufferingTime;
	}

	/**
	 * The size of a segment in the offset topic
	 *
	 * @param segmentSize
	 */
	public void setSegmentSize(int segmentSize) {
		this.segmentSize = segmentSize;
	}

	/**
	 * How long are dead records retained in the offset topic
	 *
	 * @param retentionTime
	 */
	public void setRetentionTime(int retentionTime) {
		this.retentionTime = retentionTime;
	}

	/**
	 * The replication factor of the offset topic
	 *
	 * @param replicationFactor
	 */
	public void setReplicationFactor(int replicationFactor) {
		this.replicationFactor = replicationFactor;
	}

	/**
	 * The maximum batch size for offset writes
	 *
	 * @param maxBatchSize
	 */
	public void setMaxBatchSize(int maxBatchSize) {
		this.maxBatchSize = maxBatchSize;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		ZkClient zkClient = new ZkClient(zookeeperConnect.getZkConnect(),
				Integer.parseInt(zookeeperConnect.getZkSessionTimeout()),
				Integer.parseInt(zookeeperConnect.getZkConnectionTimeout()),
				ZKStringSerializer$.MODULE$);

		try {
			createCompactedTopicIfNotFound(zkClient);
			validateOffsetTopic();
			Partition offsetPartition = new Partition(topic, 0);
			BrokerAddress offsetPartitionLeader = connectionFactory.getLeader(offsetPartition);
			readOffsetData(offsetPartition, offsetPartitionLeader);
			initializeProducer(offsetPartitionLeader);
		}
		finally {
			try {
				zkClient.close();
			}
			catch (ZkInterruptedException e) {
				log.error("Error while closing Zookeeper client", e);
			}
		}
	}

	private void createCompactedTopicIfNotFound(ZkClient zkClient) {
		Properties topicConfig = new Properties();
		topicConfig.setProperty("cleanup.policy", "compact");
		topicConfig.setProperty("delete.retention.ms", String.valueOf(retentionTime));
		topicConfig.setProperty("segment.bytes", String.valueOf(segmentSize));
		try {
			replicationFactor = 1;
			AdminUtils$.MODULE$.createTopic(zkClient, topic, 1, replicationFactor, topicConfig);
		}
		catch (TopicExistsException e) {
			log.debug("Topic already exists", e);
		}
	}

	private void validateOffsetTopic() throws Exception {
		//validate that the topic exists, but also prevent working with the topic until it's fully initialized
		// set a retry template, since operations may fail
		RetryTemplate retryValidateTopic = new RetryTemplate();
		retryValidateTopic.setRetryPolicy(new SimpleRetryPolicy(5,
				Collections.<Class<? extends Throwable>, Boolean>singletonMap(TopicNotFoundException.class, true)));
		ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
		backOffPolicy.setInitialInterval(50L);
		backOffPolicy.setMaxInterval(1000L);
		backOffPolicy.setMultiplier(2);
		retryValidateTopic.setBackOffPolicy(backOffPolicy);

		Collection<Partition> partitions = retryValidateTopic.execute(new RetryCallback<Collection<Partition>, Exception>() {
			@Override
			public Collection<Partition> doWithRetry(RetryContext context) throws Exception {
				connectionFactory.refreshLeaders(Collections.<String>emptySet());
				return connectionFactory.getPartitions(topic);
			}
		});

		if (partitions.size() > 1) {
			throw new BeanInitializationException("Offset management topic cannot have more than one partition");
		}

		//Also, validate that topic is compacted: see https://jira.spring.io/browse/XD-2641
	}

	private void readOffsetData(Partition offsetManagementPartition, BrokerAddress leader) {
		Result<Long> earliestOffsetResult =
				connectionFactory.connect(leader).fetchInitialOffset(OffsetRequest.EarliestTime(), offsetManagementPartition);
		if (earliestOffsetResult.getErrors().size() > 0) {
			throw new BeanInitializationException("Cannot initialize offset manager, unable to read earliest offset",
					ErrorMapping$.MODULE$.exceptionFor(earliestOffsetResult.getError(offsetManagementPartition)));
		}
		Result<Long> latestOffsetResult =
				connectionFactory.connect(leader).fetchInitialOffset(OffsetRequest.LatestTime(), offsetManagementPartition);
		if (latestOffsetResult.getErrors().size() > 0) {
			throw new BeanInitializationException("Cannot initialize offset manager, unable to read latest offset");
		}

		long initialOffset = earliestOffsetResult.getResult(offsetManagementPartition);
		long finalOffset = latestOffsetResult.getResult(offsetManagementPartition);

		long readingOffset = initialOffset;
		while (readingOffset < finalOffset) {
			FetchRequest fetchRequest = new FetchRequest(offsetManagementPartition, readingOffset, maxSize);
			Result<KafkaMessageBatch> receive = kafkaTemplate.receive(Collections.singleton(fetchRequest));
			if (receive.getErrors().size() > 0) {
				throw new BeanInitializationException("Error while fetching initial offsets:",
						ErrorMapping$.MODULE$.exceptionFor(receive.getError(offsetManagementPartition)));
			}
			KafkaMessageBatch result = receive.getResult(offsetManagementPartition);
			for (KafkaMessage kafkaMessage : result.getMessages()) {
				addMessageToOffsets(kafkaMessage);
				readingOffset = kafkaMessage.getMetadata().getNextOffset();
			}
			if (log.isDebugEnabled()) {
				log.debug(data.size() + " entries in the final map");
			}
			if (log.isTraceEnabled()) {
				for (Map.Entry<String, String> dataEntry : data.entrySet()) {
					log.trace(String.format("Final value for %s : %s", dataEntry.getKey(), dataEntry.getValue()));
				}
			}
		}
	}

	private void addMessageToOffsets(KafkaMessage kafkaMessage) {
		String key = kafkaMessage.getMessage() != null && kafkaMessage.getMessage().key() != null ?
				DECODER.fromBytes(Utils$.MODULE$.readBytes(kafkaMessage.getMessage().key())) : null;
		String value = kafkaMessage.getMessage() != null && kafkaMessage.getMessage().payload() != null ?
				DECODER.fromBytes(Utils$.MODULE$.readBytes(kafkaMessage.getMessage().payload())) : null;
		// empty valued strings are in fact non-values. We can't delete records from the Kafka topic
		// so we mark them as such
		if (log.isDebugEnabled()) {
			log.debug("Loading key " + key + " with value " + value);
		}
		if (null != value) {
			data.put(key, value);
		}
		else {
			if (data.containsKey(key)) {
				data.remove(key);
			}
		}
	}


	private void initializeProducer(BrokerAddress leader) throws Exception {
		ProducerMetadata<String, String> producerMetadata = new ProducerMetadata<String, String>(topic);

		producerMetadata.setValueEncoder(ENCODER);
		producerMetadata.setValueClassType(String.class);
		producerMetadata.setKeyEncoder(ENCODER);
		producerMetadata.setKeyClassType(String.class);
		producerMetadata.setPartitioner(new DefaultPartitioner(null));

		Properties additionalProps = new Properties();

		producerMetadata.setAsync(true);
		producerMetadata.setBatchNumMessages(String.valueOf(maxBatchSize));
		producerMetadata.setCompressionCodec(compressionCodec);
		additionalProps.put("request.required.acks", "1");
		additionalProps.put("queue.buffering.max.ms", String.valueOf(maxQueueBufferingTime));

		ProducerFactoryBean<String, String> producerFB
				= new ProducerFactoryBean<String, String>(producerMetadata, leader.toString(), additionalProps);

		// ignore sonar's complaint about this not being synchronized
		producer = producerFB.getObject();
	}

	@Override
	public synchronized void put(String key, String value) {
		if (log.isDebugEnabled()) {
			log.debug("Setting value for key: " + key + " to " + value);
		}
		data.put(key, value);
		producer.send(new KeyedMessage<String, String>(topic, key, value));
	}

	@Override
	public synchronized String get(String key) {
		if (log.isDebugEnabled()) {
			log.debug("Getting value for key: " + key + " is " + data.get(key));
		}
		return data.get(key);
	}

	@Override
	public synchronized String remove(String key) {
		if (log.isDebugEnabled()) {
			log.debug("Removing key: " + key);
		}
		String existingValue = data.remove(key);
		producer.send(new KeyedMessage<String, String>(topic, key, null));
		return existingValue;
	}

	@Override
	public void close() throws IOException {
		producer.close();
	}

}
