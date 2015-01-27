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

import static org.junit.Assert.*;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;

import org.springframework.integration.kafka.core.DefaultConnectionFactory;
import org.springframework.integration.kafka.core.FetchRequest;
import org.springframework.integration.kafka.core.KafkaMessageBatch;
import org.springframework.integration.kafka.core.KafkaTemplate;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.core.Result;
import org.springframework.integration.kafka.core.ZookeeperConfiguration;
import org.springframework.integration.kafka.serializer.common.StringDecoder;
import org.springframework.integration.kafka.support.ZookeeperConnect;
import org.springframework.integration.kafka.util.MessageUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.xd.dirt.integration.bus.RedisTestMessageBus;
import org.springframework.xd.dirt.integration.kafka.KafkaMessageBus;
import org.springframework.xd.dirt.integration.kafka.KafkaTestMessageBus;
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
			if (testMessageBus == null || !(testMessageBus instanceof RedisTestMessageBus)) {
				testMessageBus = new KafkaTestMessageBus(kafkaTestSupport);
			}
		}
	};

	@BeforeClass
	public static void setUpAll() {
		System.setProperty("xd.messagebus.kafka.zkAddress", kafkaTestSupport.getZkconnectstring());
		System.setProperty("xd.messagebus.kafka.brokers", kafkaTestSupport.getBrokerAddress());
		setUp("kafka");
	}

	@AfterClass
	public static void clearAll() {
		System.clearProperty("xd.messagebus.kafka.zkAddress");
		System.clearProperty("xd.messagebus.kafka.brokers");
	}

	@Override
	protected void verifyOnDemandQueues(MessageChannel y3, MessageChannel z3) {
		ZookeeperConnect zookeeperConnect = new ZookeeperConnect();
		zookeeperConnect.setZkConnect(kafkaTestSupport.getZkconnectstring());
		ZookeeperConfiguration configuration = new ZookeeperConfiguration(zookeeperConnect);
		DefaultConnectionFactory connectionFactory = new DefaultConnectionFactory(configuration);
		try {
			connectionFactory.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		KafkaTemplate template = new KafkaTemplate(connectionFactory);
		String y = receiveFromTopicForQueue(template, "queue:y");
		assertTrue(y.endsWith("y")); // bus headers
		String z = receiveFromTopicForQueue(template, "queue:z");
		assertNotNull(z);
		assertTrue(z.endsWith("z")); // bus headers
	}

	private String receiveFromTopicForQueue(KafkaTemplate template, String topicName) {
		Partition partition = new Partition(KafkaMessageBus.escapeTopicName(topicName), 0);
		Result<KafkaMessageBatch> receive = template.receive(Collections.singleton(new FetchRequest(partition, 0, 1000)));
		return MessageUtils.decodePayload(receive.getResult(partition).getMessages().get(0), new StringDecoder());
	}

}
