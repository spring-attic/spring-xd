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

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.ClassRule;

import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.util.StringUtils;
import org.springframework.xd.test.rabbit.RabbitTestSupport;

/**
 * @author Ilayaperumal Gopinathan
 */
public class RabbitTransportSparkStreamingTests extends AbstractSparkStreamingTests {

	@ClassRule
	public static RabbitTestSupport rabbitTestSupport = new RabbitTestSupport();

	protected static final List<String> queueNames = new ArrayList<String>();

	private static RabbitAdmin rabbitAdmin;

	public RabbitTransportSparkStreamingTests() {
		super("rabbit");
		rabbitAdmin = new RabbitAdmin(rabbitTestSupport.getResource());
	}

	@Override
	protected void createStream(String streamName, String stream) {
		streamOps.create(streamName, stream);
		addQueueNames(streamName, stream);
	}


	@AfterClass
	public static void cleanupRabbitQueues() {
		for (String queueName: queueNames) {
			rabbitAdmin.deleteQueue(queueName);
		}
	}

	private void addQueueNames(String streamName, String stream) {
		int numPipes = StringUtils.countOccurrencesOf(stream, "|");
		for (int i = 0; i < numPipes; i++) {
			queueNames.add("xdbus." + streamName + "."+ i);
		}
	}
}
