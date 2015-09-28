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

package org.springframework.xd.dirt.stream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.xd.dirt.integration.kafka.KafkaMessageBus.escapeTopicName;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import kafka.api.OffsetRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import org.springframework.integration.kafka.core.BrokerAddress;
import org.springframework.integration.kafka.core.DefaultConnectionFactory;
import org.springframework.integration.kafka.core.FetchRequest;
import org.springframework.integration.kafka.core.KafkaMessageBatch;
import org.springframework.integration.kafka.core.KafkaTemplate;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.core.PartitionNotFoundException;
import org.springframework.integration.kafka.core.Result;
import org.springframework.integration.kafka.core.ZookeeperConfiguration;
import org.springframework.integration.kafka.serializer.common.StringDecoder;
import org.springframework.integration.kafka.support.ZookeeperConnect;
import org.springframework.integration.kafka.util.MessageUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.xd.dirt.integration.bus.kafka.KafkaTestMessageBus;
import org.springframework.xd.test.kafka.KafkaTestSupport;

/**
 * @author Marius Bogoevici
 */
public class KafkaSingleNodeStreamDeploymentIntegrationTests extends
		AbstractSingleNodeStreamDeploymentIntegrationTests {

	@ClassRule
	public static KafkaTestSupport kafkaTestSupport = new KafkaTestSupport();

	@ClassRule
	public static ExternalResource initializeKafkaMessageBus = new ExternalResource() {
		@Override
		protected void before() {
			if (testMessageBus == null || !(testMessageBus instanceof KafkaTestMessageBus)) {
				testMessageBus = new KafkaTestMessageBus(kafkaTestSupport);
			}
		}
	};

	@BeforeClass
	public static void setUpAll() {
		System.setProperty("xd.messagebus.kafka.zkAddress", kafkaTestSupport.getZkConnectString());
		System.setProperty("xd.messagebus.kafka.brokers", kafkaTestSupport.getBrokerAddress());
		setUp("kafka");
	}

	@AfterClass
	public static void clearAll() {
		System.clearProperty("xd.messagebus.kafka.zkAddress");
		System.clearProperty("xd.messagebus.kafka.brokers");
	}

	@Test
	@Ignore("Disabled pending on resolution of https://jira.spring.io/browse/XD-3055")
	@Override
	public void testTappingWithRepeatedModulesDoesNotDuplicateMessages() {
		// do nothing
	}

	@Test
	@Ignore("Disabled pending on resolution of https://jira.spring.io/browse/XD-3055")
	@Override
	public void verifyQueueChannelsRegisteredOnDemand() throws InterruptedException {
		// do nothing
	}

	@Override
	protected void verifyOnDemandQueues(MessageChannel y3, MessageChannel z3, Map<String, Object> initialTransportState) {
		DefaultConnectionFactory defaultConnectionFactory = getDefaultConnectionFactory();
		KafkaTemplate template = new KafkaTemplate(defaultConnectionFactory);
		String y = receiveFromTopicForQueue(template, escapeTopicName("queue:y"), initialTransportState);
		assertTrue(y.endsWith("y")); // bus headers
		String z = receiveFromTopicForQueue(template, escapeTopicName("queue:z"), initialTransportState);
		assertNotNull(z);
		assertTrue(z.endsWith("z")); // bus headers
		try {
			defaultConnectionFactory.destroy();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private DefaultConnectionFactory getDefaultConnectionFactory() {
		ZookeeperConnect zookeeperConnect = new ZookeeperConnect();
		zookeeperConnect.setZkConnect(kafkaTestSupport.getZkConnectString());
		ZookeeperConfiguration configuration = new ZookeeperConfiguration(zookeeperConnect);
		DefaultConnectionFactory connectionFactory = new DefaultConnectionFactory(configuration);
		try {
			connectionFactory.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return connectionFactory;
	}

	@Override
	protected Map<String, Object> readInitialQueueState(String... queueNames) {
		DefaultConnectionFactory defaultConnectionFactory = getDefaultConnectionFactory();
		KafkaTemplate template = new KafkaTemplate(getDefaultConnectionFactory());
		Map<String, Object> initialOffsets = new HashMap<String, Object>();
		for (String queueName : queueNames) {
			String escapedTopicName = escapeTopicName(queueName);
			initialOffsets.put(escapedTopicName, getInitialOffset(template, escapedTopicName));
		}
		try {
			defaultConnectionFactory.destroy();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return initialOffsets;
	}

	private String receiveFromTopicForQueue(KafkaTemplate template, String topicName,
			Map<String, Object> initialTransportState) {
		Partition partition = new Partition(topicName, 0);
		Result<KafkaMessageBatch> receive = template.receive(Collections.singleton(new FetchRequest(partition,
				(Long) initialTransportState.get(topicName), 1000)));
		return MessageUtils.decodePayload(receive.getResult(partition).getMessages().get(0), new StringDecoder());
	}

	private long getInitialOffset(KafkaTemplate template, String topicName) {
		try {
			Partition partition = new Partition(topicName, 0);
			BrokerAddress leader = template.getConnectionFactory().getLeader(partition);
			return template.getConnectionFactory().connect(leader)
					.fetchInitialOffset(OffsetRequest.LatestTime(), partition).getResult(partition);
		}
		catch (PartitionNotFoundException e) {
			return 0;
		}
	}

}
