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


/**
 * Runs a basic suite of Syslog TCP and UDP tests on an XD Cluster instance.
 *
 * @author Glenn Renfro
 */
public class SyslogTest extends AbstractIntegrationTest {


	/**
	 * Verifies that the Syslog TCP Source captures the correct data.
	 *
	 */
	@Test
	public void testSyslogTcp() {
		String data = UUID.randomUUID().toString();
		stream(sources.syslogTcp() + XD_DELIMITER
				+ "file");
		sources.syslogTcpSource().sendBytes((data + "\r\n").getBytes());
		assertReceived(1);
		assertFileContains(data);
	}

	/**
	 * Verifies that the Syslog UDP Source captures the correct data.
	 *
	 */
	@Test
	public void testSyslogUdp() {
		String data = UUID.randomUUID().toString();
		stream(sources.syslogUdp() + XD_DELIMITER
				+ "file");
		sources.syslogUdpForContainerHost().sendBytes((data + "\r\n").getBytes());
		assertReceived(1);
		this.assertFileContains(data);
	}
}
