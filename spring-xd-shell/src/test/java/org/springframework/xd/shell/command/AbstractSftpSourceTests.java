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

package org.springframework.xd.shell.command;

import static org.junit.Assert.assertThat;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasValue;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.xd.shell.util.TestSftpServer;
import org.springframework.xd.test.fixtures.CounterSink;


/**
 * Tests for SFTP source module.
 *
 * @author Ilayaperumal Gopinathan
 */
public class AbstractSftpSourceTests extends AbstractStreamIntegrationTest {

	private static TestSftpServer server;

	@BeforeClass
	public static void setupSftpServer() throws Exception {
		server = new TestSftpServer();
		server.afterPropertiesSet();
	}

	@AfterClass
	public static void destroyServer() {
		try {
			server.destroy();
		}
		catch (Exception e) {
			// ignore
		}
	}

	@Before
	public void setUp() {
		//These test disabled for Windows
		org.junit.Assume.assumeFalse(System.getProperty("os.name").toLowerCase().startsWith("windows"));
	}

	@Test
	public void testSftpSource() {
		String streamName = generateStreamName();
		CounterSink counter = metrics().newCounterSink(streamName);
		stream().create(streamName, "sftp --user=%s --password=%s --port=%s --remoteDir=%s --localDir=%s "
						+ "--allowUnknownKeys=true | %s",
						// manual test; verify the key was added to the kh file
						// + "--allowUnknownKeys=true "
						// + "--knownHostsExpression=systemProperties[\"user.home\"]+\"/.ssh/known_hosts\" | %s",
				server.getUser(), server.getPassword(), server.getPort(), server.getSourceSftpDirectory(),
				server.getTargetLocalDirectory(), counter);
		assertThat(counter, eventually(hasValue("2")));
	}

	@Test
	public void testSftpSourceWithFileNamePattern() {
		String streamName = generateStreamName();
		CounterSink counter = metrics().newCounterSink(streamName);
		stream().create(streamName,
				"sftp --user=%s --password=%s --port=%s --remoteDir=%s --localDir=%s --pattern=%s "
						+ "--allowUnknownKeys=true | %s",
				server.getUser(), server.getPassword(), server.getPort(), server.getSourceSftpDirectory(),
				server.getTargetLocalDirectory(), "*.txt", counter);
		assertThat(counter, eventually(hasValue("1")));
	}

	@Test
	public void testSftpSourceWithFileNameRegexPattern() {
		String streamName = generateStreamName();
		CounterSink counter = metrics().newCounterSink(streamName);
		stream().create(streamName,
				"sftp --user=%s --password=%s --port=%s --remoteDir=%s --localDir=%s --regexPattern=%s "
						+ "--allowUnknownKeys=true | %s",
				server.getUser(), server.getPassword(), server.getPort(), server.getSourceSftpDirectory(),
				server.getTargetLocalDirectory(), "'" + "(\\w+)\\.(txt|text)$" + "'", counter);
		assertThat(counter, eventually(hasValue("2")));
	}
}
