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

import static org.junit.Assert.assertThat;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.exists;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasValue;

import java.util.Collections;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.integration.kafka.core.TopicNotFoundException;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.xd.shell.command.fixtures.HttpSource;
import org.springframework.xd.test.fixtures.CounterSink;
import org.springframework.xd.test.kafka.KafkaTestSupport;

import kafka.admin.AdminUtils;
import kafka.api.TopicMetadata;

/**
 * Integration tests for Kafka source and sinks.
 *
 * @author Ilayaperumal Gopinathan
 * @author Marius Bogoevici
 * @since 1.1
 */
public class AbstractKafkaSourceSinkTests extends AbstractStreamIntegrationTest {

	private String topic1ToUse;

	private String topic2ToUse;


	@Rule
	public KafkaTestSupport kafkaTestSupport = new KafkaTestSupport();

	@Before
	public void createTopic() throws Exception {
		topic1ToUse = "kafka-test-topic-" + random.nextInt();
		// create Kafka topic
		AdminUtils.createTopic(kafkaTestSupport.getZkClient(), topic1ToUse, 1, 1, new Properties());
		setupRetryTemplate(topic1ToUse);
	}

	private void setupRetryTemplate(final String topic) throws Exception {
		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new SimpleRetryPolicy(5,
				Collections.<Class<? extends Throwable>,Boolean>singletonMap(TopicNotFoundException.class, true)));
		retryTemplate.setBackOffPolicy(new ExponentialBackOffPolicy());
		retryTemplate.execute(new RetryCallback<TopicMetadata, Exception>() {
			@Override
			public TopicMetadata doWithRetry(RetryContext context) throws Exception {
				return AdminUtils.fetchTopicMetadataFromZk(topic, kafkaTestSupport.getZkClient());
			}
		});
	}

	@After
	public void deleteTopic() {
		// delete topic
		AdminUtils.deleteTopic(kafkaTestSupport.getZkClient(), topic1ToUse);
	}

	@Test
	public void testKafkaSourceAndSink() throws Exception {
		final String stringToPost = "Hi there!";
		// create stream with kafka sink
		final HttpSource httpSource = newHttpSource();
		stream().create(generateStreamName(), "%s | kafka --topic='%s' --brokerList='%s'",
				httpSource, topic1ToUse, kafkaTestSupport.getBrokerAddress());
		// create stream with kafka source
		final CounterSink counter = metrics().newCounterSink();
		stream().create(generateStreamName(), "kafka --topic='%s' --zkconnect=%s --outputType=text/plain | " +
						"filter --expression=payload.toString().contains('%s') | %s",
				topic1ToUse, kafkaTestSupport.getZkConnectString(), stringToPost, counter );
		httpSource.ensureReady().postData(stringToPost);
		assertThat(counter, eventually(exists()));
	}


	@Test
	public void testKafkaSourceAndSinkWithMultiTopics() throws Exception {
		topic2ToUse = "kafka-test-topic-" + random.nextInt();
		AdminUtils.createTopic(kafkaTestSupport.getZkClient(), topic2ToUse, 1, 1, new Properties());
		setupRetryTemplate(topic2ToUse);
		final String stringToPost = "Hi there!";
		// create stream with kafka sink
		final HttpSource httpSource1 = newHttpSource();
		final HttpSource httpSource2 = newHttpSource();
		stream().create(generateStreamName(), "%s | kafka --topic='%s' --brokerList='%s'",
				httpSource1, topic1ToUse, kafkaTestSupport.getBrokerAddress());
		stream().create(generateStreamName(), "%s | kafka --topic='%s' --brokerList='%s'",
				httpSource2, topic2ToUse, kafkaTestSupport.getBrokerAddress());
		// create stream with kafka source
		final CounterSink counter = metrics().newCounterSink();
		stream().create(generateStreamName(), "kafka --topics='%s,%s' --zkconnect=%s --outputType=text/plain | " +
				"filter --expression=payload.toString().contains('%s') | %s",
				topic1ToUse, topic2ToUse, kafkaTestSupport.getZkConnectString(), stringToPost, counter );
		httpSource1.ensureReady().postData(stringToPost);
		httpSource2.ensureReady().postData(stringToPost);
		assertThat(counter, eventually(exists()));
		assertThat(counter, eventually(hasValue("2")));
		AdminUtils.deleteTopic(kafkaTestSupport.getZkClient(), topic2ToUse);
	}


}
