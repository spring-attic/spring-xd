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

import org.springframework.xd.integration.util.DistributedFileSink;


/**
 * 
 * @author Glenn Renfro
 */
public class TCPTest extends AbstractIntegrationTest {

	private static int TCP_SINK_PORT = 42252;

	// @Test
	public void testTCPSource() throws Exception {
		String data = UUID.randomUUID().toString();
		stream(source.tcp() + XD_DELIMETER + sink.getSink(DistributedFileSink.class));

		source.tcp().sendBytes((data + "\r\n").getBytes());
		assertReceived();
		assertValid(data, sink.getSink(DistributedFileSink.class));
	}


	@Test
	public void testTCPSink() throws Exception {
		String data = UUID.randomUUID().toString();
		stream(source.tcp() + XD_DELIMETER + sink.getSink(DistributedFileSink.class));
		stream("dataSender", "trigger --payload='" + data + "'" + XD_DELIMETER + sink.tcp(TCP_SINK_PORT));

		assertReceived();
		assertValid(data, sink.getSink(DistributedFileSink.class));
	}
}
