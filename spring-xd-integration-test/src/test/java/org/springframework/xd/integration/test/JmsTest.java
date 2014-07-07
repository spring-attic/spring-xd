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
 * Runs a basic suite of JMS Source tests on an XD Cluster instance.
 *
 * @author Glenn Renfro
 */
public class JmsTest extends AbstractIntegrationTest {

	/**
	 * Verifies that the JMS retrieves the expected data.
	 *
	 * @throws Exception
	 */
	@Test
	public void testJmsBasic() throws Exception {
		String data = UUID.randomUUID().toString();
		stream(sources.jms() + XD_DELIMITER + sinks.file());
		sources.jms().ensureReady();
		sources.jms().sendData(data);
		assertValid(data, sinks.file());
		assertReceived(1);
	}

}
