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

package org.springframework.xd.dirt.core;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.Test;

import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.module.ModuleType;

/**
 * @author Patrick Peralta
 */
public class ModuleDeploymentsPathTest {

	@Test
	public void testPath() {
		String streamName = "my-stream";
		String moduleType = ModuleType.source.toString();
		String moduleLabel = "my-label";
		String moduleSequence = "1";
		String container = UUID.randomUUID().toString();
		String path = Paths.build(Paths.MODULE_DEPLOYMENTS, Paths.ALLOCATED, container,
				String.format("%s.%s.%s.%s", streamName, moduleType, moduleLabel, moduleSequence));

		ModuleDeploymentsPath moduleDeploymentsPath = new ModuleDeploymentsPath()
				.setContainer(container)
				.setDeploymentUnitName(streamName)
				.setModuleType(moduleType)
				.setModuleLabel(moduleLabel)
				.setModuleSequence(moduleSequence);

		assertEquals(path, moduleDeploymentsPath.build());

		ModuleDeploymentsPath fromPath = new ModuleDeploymentsPath(path);
		assertEquals(container, fromPath.getContainer());
		assertEquals(streamName, fromPath.getDeploymentUnitName());
		assertEquals(moduleType, fromPath.getModuleType());
		assertEquals(moduleLabel, fromPath.getModuleLabel());
		assertEquals(moduleSequence, fromPath.getModuleSequenceAsString());
	}

}
