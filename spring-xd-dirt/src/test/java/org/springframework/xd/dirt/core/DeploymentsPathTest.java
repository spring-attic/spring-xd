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

/**
 * @author Patrick Peralta
 */
public class DeploymentsPathTest {

	@Test
	public void testPath() {
		String streamName = "my-stream";
		String moduleType = Module.Type.SOURCE.toString();
		String moduleLabel = "my-label";
		String container = UUID.randomUUID().toString();
		String path = Paths.buildWithNamespace(Paths.DEPLOYMENTS, container,
				String.format("%s.%s.%s", streamName, moduleType, moduleLabel));

		DeploymentsPath deploymentsPath = new DeploymentsPath()
				.setContainer(container)
				.setStreamName(streamName)
				.setModuleType(moduleType)
				.setModuleLabel(moduleLabel);

		assertEquals(path, deploymentsPath.buildWithNamespace());

		DeploymentsPath fromPath = new DeploymentsPath(path);
		assertEquals(container, fromPath.getContainer());
		assertEquals(streamName, fromPath.getStreamName());
		assertEquals(moduleType, fromPath.getModuleType());
		assertEquals(moduleLabel, fromPath.getModuleLabel());
	}

}
