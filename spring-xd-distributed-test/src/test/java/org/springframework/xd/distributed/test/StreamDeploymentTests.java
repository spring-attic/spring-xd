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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import org.springframework.xd.rest.client.impl.SpringXDTemplate;


/**
 * Multi container stream deployment tests.
 *
 * @author Patrick Peralta
 */
public class StreamDeploymentTests extends AbstractDistributedTests {

	/**
	 * Start three containers and deploy a simple two module stream.
	 * Kill the container hosting the source module and assert that
	 * the module is deployed to one of the remaining containers.
	 *
	 * @throws Exception
	 */
	@Test
	public void testKillOneContainer() throws Exception {
		for (int i = 0; i < 3; i++) {
			startContainer();
		}

		SpringXDTemplate template = ensureTemplate();
		logger.info("Waiting for containers...");
		Map<Long, String> mapPidUuid = waitForContainers();
		logger.info("Containers running");

		String streamName = testName.getMethodName() + "-ticktock";

		template.streamOperations().createStream(streamName, "time|log", false);
		verifyStreamCreated(streamName);

		template.streamOperations().deploy(streamName, Collections.<String, String> emptyMap());
		verifyStreamDeployed(streamName);

		ModuleRuntimeContainers moduleContainers = retrieveModuleRuntimeContainers(streamName);

		// kill the source
		long pidToKill = 0;
		for (Map.Entry<Long, String> entry : mapPidUuid.entrySet()) {
			if (moduleContainers.getSourceContainers().contains(entry.getValue())) {
				pidToKill = entry.getKey();
				break;
			}
		}
		assertFalse(pidToKill == 0);
		logger.info("Killing container with pid {}", pidToKill);
		shutdownContainer(pidToKill);

		verifyStreamDeployed(streamName);

		// ensure the module is picked up by another server
		ModuleRuntimeContainers redeployedModuleContainers = retrieveModuleRuntimeContainers(streamName);
		logger.debug("old source containers:{}, new source containers: {}",
				moduleContainers.getSourceContainers(),
				redeployedModuleContainers.getSourceContainers());

		assertNotEquals(moduleContainers.getSourceContainers(),
				redeployedModuleContainers.getSourceContainers());

	}

	/**
	 * Start two containers and deploy a simple two module stream.
	 * Shut down all of the containers. Start a new container and
	 * assert that the stream modules are deployed to the new container.
	 *
	 * @throws Exception
	 */
	@Test
	public void testKillAllContainers() throws Exception {
			for (int i = 0; i < 2; i++) {
				startContainer();
			}

		SpringXDTemplate template = ensureTemplate();
		logger.info("Waiting for containers...");
		waitForContainers();
		logger.info("Containers running");

		String streamName = testName.getMethodName() + "-ticktock";
		template.streamOperations().createStream(streamName, "time|log", true);
		verifyStreamDeployed(streamName);

		// verify modules
		retrieveModuleRuntimeContainers(streamName);

		// kill all the containers
		shutdownContainers();
		Map<Long, String> map = waitForContainers();
		assertTrue(map.isEmpty());

		startContainer();
		Map<Long, String> mapPidUuid = waitForContainers();
		assertEquals(1, mapPidUuid.size());
		String containerUuid = mapPidUuid.values().iterator().next();

		verifyStreamDeployed(streamName);

		ModuleRuntimeContainers moduleContainers = retrieveModuleRuntimeContainers(streamName);
		assertTrue(moduleContainers.getSourceContainers().contains(containerUuid));
		assertTrue(moduleContainers.getSinkContainers().contains(containerUuid));
	}

}
