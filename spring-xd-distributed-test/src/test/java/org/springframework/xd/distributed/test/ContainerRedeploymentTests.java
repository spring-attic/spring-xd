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

package org.springframework.xd.distributed.test;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.springframework.integration.test.util.SocketUtils;
import org.springframework.xd.dirt.core.DeploymentUnitStatus;
import org.springframework.xd.rest.client.impl.SpringXDTemplate;

/**
 * Set of tests to verify deployment behavior when containers
 * are added to the cluster.
 *
 * @author Patrick Peralta
 */
public class ContainerRedeploymentTests extends AbstractDistributedTests {

	/**
	 * Assert correct deployment behavior when a stream is deployed
	 * prior to the availability of any containers to deploy it. The
	 * sequence is as follows:
	 * <ul>
	 *     <li>Deploy a 3 module stream</li>
	 *     <li>Assert that the stream state is "failed"</li>
	 *     <li>Start 3 containers</li>
	 *     <li>Assert that each container deployed a module from the stream</li>
	 * </ul>
	 *
	 * @throws Exception
	 */
	@Test
	public void testNewContainerDeployment() throws Exception {
		String streamName = testName.getMethodName() + "-upper-case";

		SpringXDTemplate template = ensureTemplate();
		int httpPort = SocketUtils.findAvailableServerSocket();
		template.streamOperations().createStream(streamName,
				String.format("http --port=%d | transform --expression='payload.toUpperCase()' | log", httpPort),
				false);
		verifyStreamCreated(streamName);

		template.streamOperations().deploy(streamName, Collections.<String, String> emptyMap());
		verifyStreamState(streamName, DeploymentUnitStatus.State.failed);

		for (int i = 0; i < 3; i++) {
			startContainer();
		}

		Map<Long, String> mapPidUuid = waitForContainers();

		verifyStreamDeployed(streamName);
		ModuleRuntimeContainers moduleContainers = retrieveModuleRuntimeContainers(streamName);

		assertEquals("Expected one source container",
				1, moduleContainers.getSourceContainers().size());
		assertEquals("Expected one processor container",
				1, moduleContainers.getProcessorContainers().keySet().size());
		assertEquals("Expected one sink container",
				1, moduleContainers.getSinkContainers().size());

		Set<String> targetContainers = new HashSet<String>(mapPidUuid.values());
		targetContainers.removeAll(moduleContainers.getSourceContainers());
		targetContainers.removeAll(moduleContainers.getProcessorContainers().keySet());
		targetContainers.removeAll(moduleContainers.getSinkContainers());

		assertTrue(String.format("These containers did not receive any module deployments: %s", targetContainers),
				targetContainers.isEmpty());
	}

}
