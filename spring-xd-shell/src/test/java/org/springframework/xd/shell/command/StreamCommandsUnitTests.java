/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.shell.command;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.xd.rest.client.StreamOperations;

/**
 * True unit tests for Stream commands.
 *
 * @author Eric Bottard
 */
public class StreamCommandsUnitTests {

	private StreamOperations streamOperations;

	private StreamCommands commands;

	@Before
	public void setUp() throws Exception {
		streamOperations = Mockito.mock(StreamOperations.class);
		commands = new StreamCommands() {
			@Override
			StreamOperations streamOperations() {
				return streamOperations;
			}
		};
	}

	@Test
	public void testInlineDeploymentProperties() throws Exception {
		commands.deployStream("foo", "module.bar.count = 3", null);

		Mockito.verify(streamOperations).deploy("foo", Collections.singletonMap("module.bar.count", "3"));
	}

	@Test
	public void testFileDeploymentProperties() throws Exception {
		File file = Files.createTempFile(StreamCommandsUnitTests.class.getSimpleName(), "testFileDeploymentProperties").toFile();
		file.deleteOnExit();

		Properties props = new Properties();
		props.put("module.bar.count", "3");
		props.store(new FileOutputStream(file), "deployment props");

		commands.deployStream("foo", null, file);

		Mockito.verify(streamOperations).deploy("foo", Collections.singletonMap("module.bar.count", "3"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testOnlyOneKindOfDeploymentPropertiesAllowed() throws Exception {
		File file = Files.createTempFile(StreamCommandsUnitTests.class.getSimpleName(), "testFileDeploymentProperties").toFile();
		file.deleteOnExit();

		commands.deployStream("foo", "module.bar.count=3", file);
	}
}
