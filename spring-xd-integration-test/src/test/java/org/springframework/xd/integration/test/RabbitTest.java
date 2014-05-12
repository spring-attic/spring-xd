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

package org.springframework.xd.integration.test;


import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.xd.test.fixtures.RabbitSource;


/**
 * Executes Acceptance tests against rabbit source.
 *
 * @author Glenn Renfro
 */
public class RabbitTest extends AbstractIntegrationTest {

	private RabbitSource rabbitSource;

	/**
	 * Verifies connectivity to the broker instance and creates the queue for the test.
	 */
	@Before
	public void initialize() {
		rabbitSource = sources.rabbitSource();
		String queue = UUID.randomUUID().toString();
		rabbitSource.ensureReady();
		rabbitSource.queue(queue).createQueue();
	}


	/**
	 * Verfies that a message dispatched to a queue can be picked up and properly processed by the Rabbit Source.
	 *
	 */
	@Test
	public void testRabbitSource() {
		String data = UUID.randomUUID().toString();
		stream(rabbitSource + XD_DELIMETER + sinks.file());
		waitForXD();
		rabbitSource.sendData(data);
		waitForXD();
		assertValid(data, sinks.file());
	}

	/**
	 * Destroys the queue that was created by this test.
	 */
	@After
	public void cleanup() {
		// Need to undeploy stream before destroying queue or the source will throw exceptions when the queue is
		// destroyed.
		undeployStream();
		waitForXD();
		rabbitSource.destroyQueue();
	}
}
