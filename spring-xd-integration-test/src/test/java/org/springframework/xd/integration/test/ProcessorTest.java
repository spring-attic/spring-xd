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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.springframework.xd.integration.util.InvalidResultException;
import org.springframework.xd.integration.util.Sink;
import org.springframework.xd.integration.util.Source;

/**
 * @author Glenn Renfro
 */
@RunWith(Parameterized.class)
public class ProcessorTest extends AbstractIntegrationTest {

	public ProcessorTest(Sink sink) {
		this.sink = sink;
	}

	@Test(expected = InvalidResultException.class)
	public void testFailedSink() throws Exception {
		stream(Source.HTTP + XD_DELIMETER + " filter --expression=payload=='good' " + XD_DELIMETER + sink);
		send("HTTP", "BAD");
		assertReceived();
		// assertNoResult();
	}

	@Test
	public void testFilter() throws Exception {
		String filterContent = "good";
		stream(Source.HTTP + XD_DELIMETER + " filter --expression=payload=='" + filterContent + "' " + XD_DELIMETER
				+ sink);

		send("HTTP", filterContent);
		assertReceived();
		assertValid(filterContent);
	}


}
