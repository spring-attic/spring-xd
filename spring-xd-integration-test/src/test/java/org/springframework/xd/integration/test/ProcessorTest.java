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

import org.springframework.xd.integration.util.InvalidResultException;
import org.springframework.xd.shell.command.fixtures.SimpleFileSink;


/**
 * Verifies that processors are functional on a XD Cluster Instance.
 * 
 * @author Glenn Renfro
 */
public class ProcessorTest extends AbstractIntegrationTest {


	/**
	 * Evaluates that a single data entry of "BAD" is filtered out and not stored.
	 * 
	 * @throws Exception
	 */
	@Test(expected = InvalidResultException.class)
	public void testFailedSink() throws Exception {
		String filterContent = "BAD";
		stream("trigger  --payload='" + filterContent + "'"
				+ XD_DELIMETER + " filter --expression=payload=='good' " + XD_DELIMETER
				+ sinks.getSink(SimpleFileSink.class));
		assertReceived();
	}

	/**
	 * Evaluates that a single data entry of "good" is not allowed past the filter and stored in a file..
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFilter() throws Exception {
		String filterContent = "good";
		stream("trigger  --payload='" + filterContent + "'" +
				XD_DELIMETER + " filter --expression=payload=='" + filterContent + "' " + XD_DELIMETER
				+ sinks.getSink(SimpleFileSink.class));
		assertReceived();
		assertValid(filterContent, sinks.getSink(SimpleFileSink.class));
	}

}
