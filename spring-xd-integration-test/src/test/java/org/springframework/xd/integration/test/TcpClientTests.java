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

import org.junit.Test;


/**
 * Runs a basic suite of TCPClient tests on an XD Cluster instance.
 * In this integration test an external tcp server will need to provide the test string
 * "UOHSO638abna9beqw12" upon request.  Provided in the src/scripts directory are 2
 * scripts (sampleNIX.sh and sampleOSX.sh) that will provide a sample tcp server.
 *
 * @author Glenn Renfro
 */
public class TcpClientTests extends AbstractIntegrationTest {

	/**
	 * Verifies that the TCP Client Source Retrieves a fixed string from service.
	 *
	 */
	@Test
	public void testTcpClientSource() {
		stream(sources.tcpClientSource() + XD_DELIMITER + sinks.file());
		assertFileContains("UOHSO638abna9beqw12");
	}

}
