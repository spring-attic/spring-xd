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
package org.springframework.xd.shell.command;

import static org.junit.Assert.*;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.*;

import java.util.Properties;

import kafka.admin.AdminUtils;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.xd.shell.command.fixtures.HttpSource;
import org.springframework.xd.test.fixtures.CounterSink;
import org.springframework.xd.test.kafka.KafkaTestSupport;

/**
 * Integration tests for Kafka source and sinks.
 *
 * @author Ilayaperumal Gopinathan
 */
public class KafkaSourceSinkTests extends AbstractStreamIntegrationTest{

	private static final String zkConnectString = "localhost:2181";

	@Rule
	public KafkaTestSupport kafkaTestSupport = new KafkaTestSupport(zkConnectString);

	@Test
	public void testKafkaSourceAndSink() throws Exception {
		String topicName = generateStreamName();
		final String stringToPost = "Hi there!";
		// create Kafka topic
		AdminUtils.createTopic(kafkaTestSupport.getZkClient(), topicName, 1, 1, new Properties());
		// create stream with kafka sink
		final HttpSource httpSource = newHttpSource();
		stream().create(topicName, "%s | kafka --topic='%s'", httpSource, topicName);
		Thread.sleep(1000);
		// create stream with kafka source
		final CounterSink counter = metrics().newCounterSink();
		stream().create(generateStreamName(), "kafka --topic='%s' --zkconnect=%s | " +
				"filter --expression=payload.toString().contains('%s') | %s",
				topicName, zkConnectString, stringToPost, counter );
		Thread.sleep(1000);
		httpSource.ensureReady().postData(stringToPost);
		Thread.sleep(5000);
		assertThat(counter, eventually(exists()));
		// delete topic
		AdminUtils.deleteTopic(kafkaTestSupport.getZkClient(), topicName);
	}


}
