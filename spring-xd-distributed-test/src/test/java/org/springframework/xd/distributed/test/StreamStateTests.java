/*
 * Copyright 2014-2015 the original author or authors.
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

import static org.junit.Assert.assertFalse;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.springframework.xd.dirt.core.DeploymentUnitStatus;
import org.springframework.xd.rest.client.impl.SpringXDTemplate;
import org.springframework.xd.rest.domain.support.DeploymentPropertiesFormat;

/**
 * Set of tests to verify that deployed streams are reporting the correct
 * state after starting/shutting down containers.
 *
 * @author Patrick Peralta
 */
public class StreamStateTests extends AbstractDistributedTests {

	/**
	 * For a stream with a sink that requires two containers,
	 * assert the following state transitions:
	 * <ul>
	 *     <li>Initial state: deployed</li>
	 *     <li>Shut down one container (not the one hosting
	 *         the source): incomplete</li>
	 *     <li>Shut down the remaining container: failed</li>
	 *     <li>Start one container: incomplete</li>
	 *     <li>Start another container: deployed</li>
	 * </ul>
	 *
	 * @throws Exception
	 */
	@Test
	public void testIncompleteState() throws Exception {
		for (int i = 0; i < 2; i++) {
			startContainer();
		}

		SpringXDTemplate template = ensureTemplate();
		logger.info("Waiting for containers...");
		Map<Long, String> mapPidUuid = waitForContainers();
		logger.info("Containers running");

		String streamName = testName.getMethodName() + "-ticktock";

		template.streamOperations().createStream(streamName, "time|log", false);
		verifyStreamCreated(streamName);

		template.streamOperations().deploy(streamName, DeploymentPropertiesFormat.parseDeploymentProperties("module.log.count=2"));
		verifyStreamDeployed(streamName);

		ModuleRuntimeContainers moduleContainers = retrieveModuleRuntimeContainers(streamName);
		Set<String> sinks = new HashSet<String>(moduleContainers.getSinkContainers());
		sinks.removeAll(moduleContainers.getSourceContainers());

		long pidToKill = 0;
		for (Map.Entry<Long, String> entry : mapPidUuid.entrySet()) {
			if (sinks.contains(entry.getValue())) {
				pidToKill = entry.getKey();
				break;
			}
		}
		assertFalse(pidToKill == 0);

		logger.info("Killing container with pid {}", pidToKill);
		shutdownContainer(pidToKill);
		verifyStreamState(streamName, DeploymentUnitStatus.State.incomplete);

		shutdownContainers();
		verifyStreamState(streamName, DeploymentUnitStatus.State.failed);

		startContainer();
		verifyStreamState(streamName, DeploymentUnitStatus.State.incomplete);

		startContainer();
		verifyStreamDeployed(streamName);
	}

}
