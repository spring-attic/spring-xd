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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasContentsThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.xd.shell.command.fixtures.HttpSource;
import org.springframework.xd.test.fixtures.FileSink;
import org.springframework.xd.test.rabbit.RabbitTestSupport;


/**
 * Tests for the rabbit source and sink modules.
 * 
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
public class AbstractRabbitSourceAndRabbitSinkTests extends AbstractStreamIntegrationTest {

	@ClassRule
	public static RabbitTestSupport rabbit = new RabbitTestSupport();

	private static List<String> queues = new ArrayList<String>();

	private static RabbitAdmin rabbitAdmin;

	@BeforeClass
	public static void setupRabbitAdmin() {
		rabbitAdmin = new RabbitAdmin(rabbit.getResource());
		rabbitAdmin.afterPropertiesSet();
	}

	@Test
	public void testDefaultOptions() {
		HttpSource httpSource = newHttpSource();
		FileSink fileSink = newFileSink().binary(true);

		String streamName = generateStreamName();
		Queue queue = new Queue(streamName);
		rabbitAdmin.declareQueue(queue);
		queues.add(streamName);


		stream().create(streamName, "%s | rabbit", httpSource);
		stream().create(generateStreamName(), "rabbit --queues=%s | %s", streamName, fileSink);

		httpSource.ensureReady().postData("hello rabbit");
		assertThat(fileSink, eventually(hasContentsThat(equalTo("hello rabbit"))));
	}

	@AfterClass
	public static void cleanupQueues() {
		for (String queue : queues) {
			rabbitAdmin.deleteQueue(queue);
		}
	}

}
