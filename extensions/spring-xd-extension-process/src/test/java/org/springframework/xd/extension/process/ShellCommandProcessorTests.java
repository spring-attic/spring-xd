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

package org.springframework.xd.extension.process;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.integration.ip.tcp.serializer.AbstractByteArraySerializer;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.integration.ip.tcp.serializer.ByteArrayLfSerializer;

/**
 * @author David Turanski
 */
public class ShellCommandProcessorTests {

	private ShellCommandProcessor scp = null;

	private AbstractByteArraySerializer serializer = new ByteArrayCrLfSerializer();

	@Rule
	public PythonAvailableRule pythonAvailableRule = new PythonAvailableRule();

	@BeforeClass
	public static void init() {
		File file = new File("src/test/resources/echo.py");
		assertTrue(file.exists());
	}

	@Test
	public void echoTest() throws Exception {
		scp = new ShellCommandProcessor(serializer, "python src/test/resources/echo.py");
		scp.afterPropertiesSet();
		scp.start();
		doEchoTest();
	}

	@Test
	public void doConcurrentTest() throws Exception {
		scp = new ShellCommandProcessor(serializer, "python src/test/resources/echo.py");
		scp.afterPropertiesSet();
		scp.start();
		CountDownLatch latch = new CountDownLatch(2);
		Thread t1 = new Thread(new ShellRunner("hello", scp, latch));
		Thread t2 = new Thread(new ShellRunner("world", scp, latch));
		t1.start();
		t2.start();
		latch.await();
	}

    @Test
    public void quotedStringTest() throws Exception {
        scp = new ShellCommandProcessor(serializer, "bash -c 'while read LINE ; do echo $LINE ; done'");
        scp.afterPropertiesSet();
        scp.start();

        assertEquals("Test Message", scp.sendAndReceive("Test Message"));
    }

	static class ShellRunner implements Runnable {
		private final String message;

		private final long delay = 50;

		private final int iterations = 100;

		private final ShellCommandProcessor shellCommandProcessor;

		private final CountDownLatch latch;

		public ShellRunner(String message, ShellCommandProcessor shellCommandProcessor, CountDownLatch latch) {
			this.shellCommandProcessor = shellCommandProcessor;
			this.message = message;
			this.latch = latch;
		}

		@Override
		public void run() {
			for (int i = 0; i < iterations; i++) {
				String response = shellCommandProcessor.sendAndReceive(message);
				assertEquals(message, response);
				try {
					Thread.sleep(delay);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			latch.countDown();
		}
	}


	@Test
	public void echoTestWithLFEncoder() throws Exception {
		scp = new ShellCommandProcessor(new ByteArrayLfSerializer(),
				"python src/test/resources/echo.py");
		scp.afterPropertiesSet();
		scp.start();
		doEchoTest();
	}

	@Test
	public synchronized void testWorkingDirectory() throws Exception {

		scp = new ShellCommandProcessor(new ByteArrayLfSerializer(), "python cwd.py");
		String workingDirectory = new File("src/test/resources").getAbsolutePath();
		scp.setWorkingDirectory(workingDirectory);
		scp.afterPropertiesSet();
		scp.start();
		String cwd = scp.receive();
	}

	@Test
	public synchronized void testEnvironment() throws Exception {

		scp = new ShellCommandProcessor(new ByteArrayLfSerializer(), "python src/test/resources/env.py");
		Map<String, String> environment = new HashMap<String, String>();
		environment.put("FOO", "foo");
		environment.put("BAR", "bar");
		scp.setEnvironment(environment);
		scp.afterPropertiesSet();
		scp.start();

		String foobar = scp.receive();
		assertEquals("foobar", foobar);
	}

	@Test
	public void testError() throws Exception {
		scp = new ShellCommandProcessor(serializer, "python doesnotexist.py");
		scp.afterPropertiesSet();
		scp.start();
	}

	private void doEchoTest() {
		assertTrue(scp.isRunning());
		String response = scp.sendAndReceive("hello");
		assertEquals("hello", response);
		response = scp.sendAndReceive("echo");
		assertEquals("echo", response);
	}


	@Test
	public void testUTF8() throws Exception {
		scp = new ShellCommandProcessor(serializer, "python src/test/resources/echo.py");
		scp.setCharset("UTF-8");
		scp.afterPropertiesSet();
		scp.start();
		String response = scp.sendAndReceive("hello\u00F6\u00FF");
		assertEquals("hello\u00F6\u00FF", response);
	}

}

