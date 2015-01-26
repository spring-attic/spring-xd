/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.integration.test;

import kafka.admin.AdminUtils;
import org.I0Itec.zkclient.ZkClient;
import org.junit.Test;
import org.springframework.xd.test.fixtures.KafkaSink;
import org.springframework.xd.test.fixtures.KafkaSource;

import java.util.Properties;
import java.util.UUID;

/**
 * Runs a basic suite of Kafka Source and Sink tests on an XD Cluster instance.
 *
 * @author Glenn Renfro
 */
public class KafkaTests extends AbstractIntegrationTest {

	/**
	 * Verifies that the data sent via kafka sink can be picked up by the kafka source.
	 */
	@Test
	public void kafkaSourceSinkTest() {
		String data = UUID.randomUUID().toString();
		String topicToUse = UUID.randomUUID().toString();
		ZkClient zkClient = sources.getKafkaZkClient();
		AdminUtils.createTopic(zkClient, topicToUse, 1, 1, new Properties());

		String sinkStreamName = "ks" + UUID.randomUUID().toString();
		KafkaSource source = sources.kafkaSource().topic(topicToUse).ensureReady();
		KafkaSink sink = sinks.kafkaSink().topic(topicToUse).ensureReady();

		stream(source + XD_DELIMITER + sinks.file());
		stream(sinkStreamName, sources.http() + XD_DELIMITER + sink);
		sources.httpSource(sinkStreamName).postData(data);
		assertFileContains(data);
		assertReceived(1);
	}

}
