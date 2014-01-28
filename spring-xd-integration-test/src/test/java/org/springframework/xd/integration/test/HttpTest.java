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

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author Glenn Renfro
 */
@RunWith(Parameterized.class)
public class HttpTest extends AbstractIntegrationTest{

	private String sink;

	public HttpTest(String sink) {
		this.sink = sink;
	}

	@Parameters
	public static Collection<Object[]> sink() {
		Object[][] sink = { { "log" }, {"file"} };
		return Arrays.asList(sink);
	}

	@Test
	public void testHttp() throws Exception {
		stream("http  | " + sink);
		send("HTTP", "Hello World");
		assertReceived();
	}

}
