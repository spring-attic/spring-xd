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

import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.integration.ip.tcp.serializer.AbstractByteArraySerializer;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;

/**
 * @author David Turanski
 * @since 1.0
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
	public void testConsumingInitialInput() {
		pep = new ShellCommandProcessor(serializer,"python src/test/resources/echo.py -v");
		pep.start();
		pep.receive();
		doTest();
		pep.stop();
		assertFalse(pep.isRunning());
	}

	@Test
	public void testWithNoInitialInput() {
		pep = new ShellCommandProcessor(serializer, "python", "src/test/resources/echo.py");
		pep.start();
		doTest();
		pep.stop();
		assertFalse(pep.isRunning());
	}

	private void doTest() {
		assertTrue(pep.isRunning());
		String response = pep.sendAndReceive("hello");
		assertEquals("hello",response);
		response = pep.sendAndReceive("echo");
		assertEquals("echo",response);
	}
}
