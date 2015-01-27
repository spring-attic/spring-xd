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

package org.springframework.xd.spark.streaming;

import java.util.Properties;

import kafka.utils.TestUtils;

import org.junit.ClassRule;

import org.springframework.xd.dirt.integration.bus.KafkaConnectionPropertyNames;
import org.springframework.xd.test.kafka.KafkaTestSupport;

/**
 * @author Ilayaperumal Gopinathan
 */
public class KafkaTransportSparkStreamingTests extends AbstractSparkStreamingTests {

	@ClassRule
	public static KafkaTestSupport kafkaTestSupport;

	static {
		kafkaTestSupport = new KafkaTestSupport();
		Properties brokerConfigProperties = TestUtils.createBrokerConfig(0, TestUtils.choosePort());
		kafkaTestSupport.setBrokerConfig(brokerConfigProperties);
		System.setProperty(KafkaConnectionPropertyNames.KAFKA_BROKERS,
				brokerConfigProperties.getProperty("host.name") + ":" + brokerConfigProperties.getProperty("port"));
		System.setProperty(KafkaConnectionPropertyNames.KAFKA_ZK_ADDRESS,
				brokerConfigProperties.getProperty("zookeeper.connect"));
	}

	public KafkaTransportSparkStreamingTests() {
		super("kafka");
	}
}
