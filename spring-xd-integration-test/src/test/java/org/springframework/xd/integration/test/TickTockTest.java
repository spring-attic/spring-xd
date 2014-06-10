/*
 * Copyright 2011-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
 * Test using Time as a source.
 *
 * @author Glenn Renfro
 */

public class TickTockTest extends AbstractIntegrationTest {

	/**
	 * Verifies that a time source will generate a message upon deploy
	 */
	@Test
	public void testHeartBeat() {
		stream("time --fixedDelay=30 " + XD_DELIMITER + sinks.file());
		assertReceived(1);
	}

}
