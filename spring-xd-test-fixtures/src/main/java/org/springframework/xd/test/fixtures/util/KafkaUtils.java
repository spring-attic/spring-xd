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
package org.springframework.xd.test.fixtures.util;

import java.util.List;
import java.util.Properties;

import org.I0Itec.zkclient.ZkClient;

import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.CompositeRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import kafka.admin.AdminUtils;
import kafka.api.TopicMetadata;
import kafka.common.ErrorMapping;
import kafka.javaapi.PartitionMetadata;
import kafka.utils.ZKStringSerializer$;

/**
 * Utility methods for working with Kafka.  Used by {@link org.springframework.xd.test.fixtures.KafkaSource} and
 * {@link org.springframework.xd.test.fixtures.KafkaSink}
 *
 * @author Mark Pollack
 * @author Gary Russell
 */
public class KafkaUtils {

	private static final int METADATA_VERIFICATION_TIMEOUT = 5000;

	private static final int METADATA_VERIFICATION_RETRY_ATTEMPTS = 10;

	private static final double METADATA_VERIFICATION_RETRY_BACKOFF_MULTIPLIER = 1.5;

	private static final int METADATA_VERIFICATION_RETRY_INITIAL_INTERVAL = 100;

	private static final int METADATA_VERIFICATION_MAX_INTERVAL = 1000;

    /**
     * Ensures that the host and port of the zookeeper connection are available and
	 * that the topic has been created.
     * @param fixtureName  The name of the fixture calling htis utility method, used for error reporting
     * @param zkConnect The Zookeeper connection string
     * @param topic The name of the topic to create
     */
    public static void ensureReady(String fixtureName, String zkConnect, final String topic) {
        String[] addressArray = StringUtils.commaDelimitedListToStringArray(zkConnect);
        for (String address : addressArray) {
            String[] zkAddressArray = StringUtils.delimitedListToStringArray(address, ":");
            Assert.isTrue(zkAddressArray.length == 2,
                    "zkConnect data was not properly formatted");
            String host = zkAddressArray[0];
            int port = Integer.valueOf(zkAddressArray[1]);
            AvailableSocketPorts.ensureReady(fixtureName, host, port, 2000);
        }
        final ZkClient zkClient = new ZkClient(zkConnect, 6000, 6000, ZKStringSerializer$.MODULE$);
        AdminUtils.createTopic(zkClient, topic, 1, 1, new Properties());

		RetryTemplate retryTemplate = new RetryTemplate();

		CompositeRetryPolicy policy = new CompositeRetryPolicy();
		TimeoutRetryPolicy timeoutRetryPolicy = new TimeoutRetryPolicy();
		timeoutRetryPolicy.setTimeout(METADATA_VERIFICATION_TIMEOUT);
		SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
		simpleRetryPolicy.setMaxAttempts(METADATA_VERIFICATION_RETRY_ATTEMPTS);
		policy.setPolicies(new RetryPolicy[]{timeoutRetryPolicy, simpleRetryPolicy});
		retryTemplate.setRetryPolicy(policy);

		ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
		backOffPolicy.setInitialInterval(METADATA_VERIFICATION_RETRY_INITIAL_INTERVAL);
		backOffPolicy.setMultiplier(METADATA_VERIFICATION_RETRY_BACKOFF_MULTIPLIER);
		backOffPolicy.setMaxInterval(METADATA_VERIFICATION_MAX_INTERVAL);
		retryTemplate.setBackOffPolicy(backOffPolicy);

		try {
			retryTemplate.execute(new RetryCallback<Void, Exception>() {
				@Override
				public Void doWithRetry(RetryContext context) throws Exception {
					TopicMetadata topicMetadata = AdminUtils.fetchTopicMetadataFromZk(topic, zkClient);
					if (topicMetadata.errorCode() != ErrorMapping.NoError() || !topic.equals(topicMetadata.topic())) {
						// downcast to Exception because that's what the error throws
						throw (Exception) ErrorMapping.exceptionFor(topicMetadata.errorCode());
					}
					List<PartitionMetadata> partitionMetadatas = new kafka.javaapi.TopicMetadata(topicMetadata).partitionsMetadata();
					for (PartitionMetadata partitionMetadata : partitionMetadatas) {
						if (partitionMetadata.errorCode() != ErrorMapping.NoError()) {
							throw (Error) ErrorMapping.exceptionFor(partitionMetadata.errorCode());
						}
					}
					return null;
				}
			});
		}
		catch (Exception e) {
			throw new IllegalStateException("Unable to create topic for kafka", e);
		}
	}
}
