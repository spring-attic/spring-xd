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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.shell.core.CommandResult;

import static org.junit.Assert.*;

/**
 * Tap commands tests.
 * 
 * @author Ilayaperumal Gopinathan
 * 
 */
public class TapCommandTests extends AbstractStreamIntegrationTest {

	private static final Log logger = LogFactory.getLog(TapCommandTests.class);

	@Test
	public void testCreateTap() {
		logger.info("Create a tap");
		String streamName = "taptestticktock";
		executeStreamCreate(streamName, "http --port=" + DEFAULT_HTTP_PORT + " | log");
		executeTapCreate("taptest-tap", "tap@ " + streamName + " | file", false);
	}

	@Test
	public void testCreateAndDeployTap() throws Exception {
		logger.info("Create and deploy a tap");
		String streamName = "taptestticktock";
		String httpPort = DEFAULT_HTTP_PORT;
		executeStreamCreate(streamName, "http --port=" + httpPort + " | log");
		executeTapCreate("taptest-tap", "tap@ " + streamName + " | counter --name=" + DEFAULT_METRIC_NAME);
		// Verify tap by checking counter value after posting http data
		// Adding a small delay here to make sure the http source
		// is actually started.
		Thread.sleep(5000);

		httpPostData("http://localhost:" + httpPort, "test");
		verifyCounter("1");
	}

	@Test
	public void testDestroyTap() {
		logger.info("Destroy a tap");
		String streamName = "taptestticktock";
		String counterName = "taptest-counter" + Math.random();
		String httpPort = DEFAULT_HTTP_PORT;
		String tapName = "tapdestroytest";
		executeStreamCreate(streamName, "http --port=" + httpPort + " | log");
		String tapDefinition = "tap@ " + streamName + " | counter --name=" + counterName;
		CommandResult cr = executeCommand("tap create --definition \"" + tapDefinition + "\" --name " + tapName
				+ " --deploy true");
		assertEquals("Created and deployed new tap " + "'" + tapName + "'", cr.getResult());
		cr = executeCommand("tap destroy --name " + tapName);
		assertEquals("Destroyed tap " + "'" + tapName + "'", cr.getResult());
	}

}