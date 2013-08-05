/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.xd.shell.command;

import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

/**
 * Tests for named channels
 * 
 * @author Ilayaperumal Gopinathan
 * 
 */
public class NamedChannelTests extends AbstractStreamIntegrationTest {

	private static final Log logger = LogFactory
			.getLog(StreamCommandTests.class);

	@Test
	public void testCreateNamedChannelAsSink() {
		logger.info("Creating stream with named channel 'foo' as sink");
		// Chosen port 9193 randomly assuming this port is not being used
		// already.
		executeStreamCreate("namedchanneltest-ticktock",
				"http --port=9193 | transform --expression=payload.toUpperCase() > :foo");
	}

	@Test
	public void testCreateNamedChannelAsSource() throws InterruptedException {
		logger.info("Creating stream with named channel 'foo' as source");
		String stream1 = "namedchanneltest-ticktock";
		String stream2 = "namedchanneltest-ticktock-counter";
		String httpPort = "9193";

		String counterName = "namedchanneltest-counter" + new Random().nextInt();
		executeStreamCreate(stream1, "http --port=" + httpPort
				+ " | transform --expression=payload.toUpperCase() > :foo");
		// Create stream with named channel as source
		Thread.sleep(4000);
		executeStreamCreate(stream2, ":foo > counter --name=" + counterName);
		httpPostData("http://localhost:" + httpPort, "test");
		checkIfCounterExists(counterName);
		checkCounterValue(counterName, "1");
		deleteCounter(counterName);
	}

}