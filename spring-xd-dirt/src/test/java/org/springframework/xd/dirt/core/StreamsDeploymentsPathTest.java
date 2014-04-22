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
import static org.junit.Assert.assertNull;

import java.util.UUID;

import org.junit.Test;

import org.springframework.xd.dirt.zookeeper.Paths;

/**
 * @author Patrick Peralta
 */
public class StreamsDeploymentsPathTest {

	@Test
	public void testFullPath() {
		String streamName = "my-stream";
		String moduleType = Module.Type.SOURCE.toString();
		String moduleLabel = "my-label";
		String container = UUID.randomUUID().toString();
		String path = Paths.buildWithNamespace(Paths.STREAM_DEPLOYMENTS, streamName, moduleType, moduleLabel, container);

		StreamsDeploymentsPath streamsDeploymentsPath = new StreamsDeploymentsPath(path);

		assertEquals(streamName, streamsDeploymentsPath.getStreamName());
		assertEquals(moduleType, streamsDeploymentsPath.getModuleType());
		assertEquals(moduleLabel, streamsDeploymentsPath.getModuleLabel());
		assertEquals(container, streamsDeploymentsPath.getContainer());


		StreamsDeploymentsPath streamsDeploymentsPathEmptyCtor = new StreamsDeploymentsPath()
				.setStreamName(streamName)
				.setModuleType(moduleType)
				.setModuleLabel(moduleLabel)
				.setContainer(container);

		assertEquals(path, streamsDeploymentsPathEmptyCtor.buildWithNamespace());
	}

	@Test
	public void testStreamNameOnly() {
		String streamName = "my-stream";
		String path = Paths.build(Paths.STREAM_DEPLOYMENTS, streamName);
		StreamsDeploymentsPath streamsDeploymentsPath = new StreamsDeploymentsPath().setStreamName(streamName);

		assertEquals(path, streamsDeploymentsPath.build());

		StreamsDeploymentsPath fromPath = new StreamsDeploymentsPath(path);
		assertEquals(streamName, fromPath.getStreamName());
		assertNull(fromPath.getModuleType());
		assertNull(fromPath.getModuleLabel());
		assertNull(fromPath.getContainer());
	}
}
