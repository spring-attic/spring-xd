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
 * Unit tests for {@link StreamDeploymentsPath}.
 *
 * @author Patrick Peralta
 */
public class StreamDeploymentsPathTest {

	/**
	 * Test the usage of {@link StreamDeploymentsPath} when
	 * providing the entire path. Assert that the path is parsed
	 * and generated as expected.
	 */
	@Test
	public void testFullPath() {
		String streamName = "my-stream";
		String moduleType = ModuleType.source.toString();
		String moduleLabel = "my-label";
		String moduleSequence = "0";
		String container = UUID.randomUUID().toString();
		String path = Paths.buildWithNamespace(Paths.STREAM_DEPLOYMENTS, streamName, Paths.MODULES,
				String.format("%s.%s.%s.%s", moduleType, moduleLabel, moduleSequence, container));

		StreamDeploymentsPath streamDeploymentsPath = new StreamDeploymentsPath(path);

		assertEquals(streamName, streamDeploymentsPath.getStreamName());
		assertEquals(moduleType, streamDeploymentsPath.getModuleType());
		assertEquals(moduleLabel, streamDeploymentsPath.getModuleLabel());
		assertEquals(moduleSequence, streamDeploymentsPath.getModuleSequence());
		assertEquals(container, streamDeploymentsPath.getContainer());

		StreamDeploymentsPath streamDeploymentsPathEmptyCtor = new StreamDeploymentsPath()
				.setStreamName(streamName)
				.setModuleType(moduleType)
				.setModuleLabel(moduleLabel)
				.setModuleSequence(moduleSequence)
				.setContainer(container);

		assertEquals(path, streamDeploymentsPathEmptyCtor.buildWithNamespace());
	}

	/**
	 * Assert that an {@link java.lang.IllegalStateException} is thrown
	 * when attempting to build an incomplete {@link StreamDeploymentsPath}.
	 */
	@Test(expected = IllegalStateException.class)
	public void testStreamNameOnly() {
		String streamName = "my-stream";
		StreamDeploymentsPath streamDeploymentsPath = new StreamDeploymentsPath().setStreamName(streamName);

		streamDeploymentsPath.build();
	}
}
