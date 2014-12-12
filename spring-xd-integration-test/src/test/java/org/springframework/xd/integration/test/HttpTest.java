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

import java.util.UUID;

import org.junit.Test;

import org.springframework.xd.test.fixtures.AbstractModuleFixture;

/**
 * Test both the http sink and http source.
 *
 * @author Glenn Renfro
 */
public class HttpTest extends AbstractIntegrationTest {

	/**
	 * Listens for data via the http source and outputs the data to a file sink. The file is checked to see if it
	 * contains the exact data. If exact the test is successful. Else an assertion is thrown.
	 *
	 */
	@Test
	public void testHttpFileSink() {
		genericTest(sinks.file());
	}

	/**
	 * Listens for data via the http source and outputs the data to a log sink. The log is searched for the original
	 * data. If found it is successful. Else an assertion is thrown.
	 *
	 */
	@Test
	public void testHttplogSink() {
		genericTest(sinks.log());
	}


	private void genericTest(AbstractModuleFixture<?> sink) {
		String data = UUID.randomUUID().toString();
		stream(sources.http() + XD_DELIMITER + sink);
		sources.httpSource().postData(data);
		assertValid(data, sink);
		assertReceived(1);


	}

}
