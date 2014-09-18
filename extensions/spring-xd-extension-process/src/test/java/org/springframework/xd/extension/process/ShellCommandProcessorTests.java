package org.springframework.xd.extension.process;/*
 *
 *  * Copyright 2014 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.integration.ip.tcp.serializer.AbstractByteArraySerializer;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.integration.ip.tcp.serializer.ByteArrayLfSerializer;

/**
 * @author David Turanski
 */
public class ShellCommandProcessorTests {
	private ShellCommandProcessor pep = null;

	private AbstractByteArraySerializer serializer = new ByteArrayCrLfSerializer();

	@BeforeClass
	public static void init() {
		File file = new File("src/test/resources/echo.py");
		assertTrue(file.exists());
	}

	@Test
	public void testConsumingInitialOutput() throws Exception {
		pep = new ShellCommandProcessor(serializer, "python src/test/resources/echo.py -v");
		pep.afterPropertiesSet();
		pep.start();
		System.out.println(pep.receive());
		doEchoTest();
		pep.stop();
		assertFalse(pep.isRunning());
	}

	@Test
	public void testWithNoInitialOutput() throws Exception {
		pep = new ShellCommandProcessor(serializer, "python src/test/resources/echo.py");
		pep.afterPropertiesSet();
		pep.start();
		doEchoTest();
		pep.stop();
		assertFalse(pep.isRunning());
	}

	@Test
	public void testWorkingDirectory() throws Exception {
		pep = new ShellCommandProcessor(new ByteArrayLfSerializer(), "python cwd.py");
		String workingDirectory = new File("src/test/resources").getAbsolutePath();
		pep.setWorkingDirectory(workingDirectory);
		pep.afterPropertiesSet();
		pep.start();

		String cwd = pep.receive();
		assertEquals(workingDirectory, cwd);
		pep.stop();
		assertFalse(pep.isRunning());
	}

	@Test
	public void testEnvironment() throws Exception {

		pep = new ShellCommandProcessor(new ByteArrayLfSerializer(), "python src/test/resources/env.py");
		Map<String, String> environment = new HashMap<String, String>();
		environment.put("FOO", "foo");
		environment.put("BAR", "bar");
		pep.setEnvironment(environment);
		pep.afterPropertiesSet();
		pep.start();

		String foobar = pep.receive();
		assertEquals("foobar", foobar);
		pep.stop();
		assertFalse(pep.isRunning());
	}

	@Test
	public void testError() throws Exception {
		pep = new ShellCommandProcessor(new ByteArrayLfSerializer(), "python doesnotexist.py");
		pep.afterPropertiesSet();
		pep.start();
		pep.stop();
		assertFalse(pep.isRunning());
	}

	private void doEchoTest() {
		assertTrue(pep.isRunning());
		String response = pep.sendAndReceive("hello");
		assertEquals("hello", response);
		response = pep.sendAndReceive("echo");
		assertEquals("echo", response);
	}
}
