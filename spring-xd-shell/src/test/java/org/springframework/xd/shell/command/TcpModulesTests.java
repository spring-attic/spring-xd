/*
 * Copyright 2013 the original author or authors.
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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasContentsThat;

import org.junit.Test;

import org.springframework.xd.shell.command.fixtures.FileSink;
import org.springframework.xd.shell.command.fixtures.HttpSource;
import org.springframework.xd.shell.command.fixtures.TcpSink;
import org.springframework.xd.shell.command.fixtures.TcpSource;


/**
 * Tests for tcp source and sink.
 * 
 * @author Eric Bottard
 */
public class TcpModulesTests extends AbstractStreamIntegrationTest {

	@Test
	public void testTcpSource() throws Exception {
		TcpSource tcpSource = newTcpSource();
		FileSink fileSink = newFileSink().binary(true);
		stream().create(generateStreamName(), "%s | %s", tcpSource, fileSink);

		// Following \r\n is because of CRLF deserializer
		tcpSource.ensureReady().sendBytes("Hello\r\n".getBytes("UTF-8"));
		assertThat(fileSink, eventually(hasContentsThat(equalTo("Hello"))));

	}


	@Test
	public void testTcpSink() throws Exception {
		TcpSink tcpSink = newTcpSink().start();
		HttpSource httpSource = newHttpSource();


		stream().create(generateStreamName(), "%s | %s", httpSource, tcpSink);
		httpSource.ensureReady().postData("Hi there!");
		// The following CRLF is b/c of the default tcp serializer
		// NOT because of FileSink
		assertEquals("Hi there!\r\n", new String(tcpSink.getReceivedBytes(), "ISO-8859-1"));

	}
}
