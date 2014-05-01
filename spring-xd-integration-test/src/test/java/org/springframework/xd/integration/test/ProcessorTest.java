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
 * Verifies that processors are functional on a XD Cluster Instance.
 *
 * @author Glenn Renfro
 */
public class ProcessorTest extends AbstractIntegrationTest {


	/**
	 * Evaluates that a single data entry of "BAD" is filtered out and not stored.
	 *
	 */
	@Test(expected = AssertionError.class)
	public void testFailedSink() {
		String filterContent = "BAD";
		stream("trigger  --payload='" + filterContent + "'"
				+ XD_DELIMETER + " filter --expression=payload=='good' " + XD_DELIMETER
				+ sinks.file());
		assertReceived(1);
	}

	/**
	 * Evaluates that a single data entry of "good" is not allowed past the filter and stored in a file..
	 *
	 */
	@Test
	public void testFilter() {
		String filterContent = "good";
		stream("trigger  --payload='" + filterContent + "'" +
				XD_DELIMETER + " filter --expression=payload=='" + filterContent + "' " + XD_DELIMETER
				+ sinks.file());
		assertReceived(1);
		assertValid(filterContent, sinks.file());
	}

}
