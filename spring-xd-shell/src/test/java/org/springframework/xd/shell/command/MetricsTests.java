/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.xd.shell.command;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests various metrics related sinks.
 * 
 * @author Eric Bottard
 */
public class MetricsTests extends AbstractStreamIntegrationTest {

	@Test
	// pending a way to reset counters
	@Ignore
	public void testSimpleCounter() throws Exception {
		executeStreamCreate("foo", "http --port=9193 | counter --name=bar");

		executeCommand("http post http://localhost:9193 --data one");
		executeCommand("http post http://localhost:9193 --data one");
		executeCommand("http post http://localhost:9193 --data two");

		String result = (String) executeCommand("counter display bar").getResult();
		Assert.assertEquals("3", result);

	}

	@Test
	// pending a way to reset counters
	@Ignore
	public void testSimpleCounterImplicitName() throws Exception {
		executeStreamCreate("foo", "http --port=9193 | counter");

		executeCommand("http post http://localhost:9193 --data one");

		String result = (String) executeCommand("counter display foo").getResult();
		Assert.assertEquals("1", result);
	}

}
