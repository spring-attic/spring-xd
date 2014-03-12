/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.integration.test;

import java.util.UUID;

import org.junit.Test;

import org.springframework.xd.integration.fixtures.FileSink;


/**
 * Runs a basic suite of TCP (Source/Sink) tests on an XD Cluster instance.
 * 
 * @author Glenn Renfro
 */
public class TcpTest extends AbstractIntegrationTest {


	/**
	 * Verifies that the TCP Source that terminates with a CRLF returns the correct data.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTCPSourceCRLF() throws Exception {
		String data = UUID.randomUUID().toString();
		System.out.println(sources.tcp() + XD_DELIMETER + sinks.getSink(FileSink.class));
		stream(sources.tcp() + XD_DELIMETER + sinks.getSink(FileSink.class));

		sources.tcp().sendBytes((data + "\r\n").getBytes());
		assertReceived();
		assertValid(data, sinks.getSink(FileSink.class));
	}

	/**
	 * * Verifies that data sent by the TCP sink that terminates with a CRLF works as expected.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTCPSink() throws Exception {
		String data = UUID.randomUUID().toString();
		stream(sources.tcp() + XD_DELIMETER + sinks.getSink(FileSink.class));
		stream("dataSender", "trigger --payload='" + data + "'" + XD_DELIMETER + sinks.tcp());

		assertReceived();
		assertValid(data, sinks.getSink(FileSink.class));
	}
}
