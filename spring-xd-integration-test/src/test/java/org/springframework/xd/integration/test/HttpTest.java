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
import org.springframework.xd.test.fixtures.LogSink;
import org.springframework.xd.test.fixtures.SimpleFileSink;

/**
 * @author Glenn Renfro
 */
public class HttpTest extends AbstractIntegrationTest {

	// Removed the parameratized test because requires only one zero argument public constructor

	/**
	 * Test the http source for retrieving information, and outputs the data to the file sink.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testHttpFileSink() throws Exception {
		genericTest(sinks.getSink(SimpleFileSink.class));
	}

	/**
	 * Test the http source for retrieving information, and outputs the data to the log sink.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testHttplogSink() throws Exception {
		genericTest(sinks.getSink(LogSink.class));
	}


	private void genericTest(AbstractModuleFixture sink) throws Exception {
		String data = UUID.randomUUID().toString();
		stream(sources.http() + XD_DELIMETER + sink);
		waitForXD();
		sources.http().postData(data);
		waitForXD(2000);
		assertReceived(1);
		assertValid(data, sink);

	}

}
