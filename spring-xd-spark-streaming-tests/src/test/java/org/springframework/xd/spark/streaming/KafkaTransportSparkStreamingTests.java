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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import org.springframework.xd.test.kafka.KafkaTestSupport;

/**
 * @author Ilayaperumal Gopinathan
 */
public class KafkaTransportSparkStreamingTests extends AbstractSparkStreamingTests {

	@ClassRule
	public static final KafkaTestSupport kafkaTestSupport = new KafkaTestSupport();

	private static final String KAFKA_BROKERS = "xd.messagebus.kafka.brokers";

	private static final String KAFKA_ZK_ADDRESS = "xd.messagebus.kafka.zkAddress";

	private static String originalKafkaBrokers = null;

	private static String originalKafkaZkAddress = null;

	@BeforeClass
	public static void setUpClass() {
		originalKafkaBrokers = System.getProperty(KAFKA_BROKERS);
		originalKafkaZkAddress = System.getProperty(KAFKA_ZK_ADDRESS);
		System.setProperty(KAFKA_BROKERS, kafkaTestSupport.getBrokerAddress());
		System.setProperty(KAFKA_ZK_ADDRESS, kafkaTestSupport.getZkConnectString());
	}

	@AfterClass
	public static void tearDownClass() {
		if (originalKafkaBrokers == null) {
			System.clearProperty(KAFKA_BROKERS);
		}
		else {
			System.setProperty(KAFKA_BROKERS, originalKafkaBrokers);
		}
		if (originalKafkaZkAddress == null) {
			System.clearProperty(KAFKA_ZK_ADDRESS);
		}
		else {
			System.setProperty(KAFKA_ZK_ADDRESS, originalKafkaZkAddress);
		}
	}

	public KafkaTransportSparkStreamingTests() {
		super("kafka");
	}
}
