/*
 * Copyright 2014 the original author or authors.
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

import org.junit.Test;

import org.springframework.xd.test.fixtures.TwitterSearchSource;


/**
 * Test Twitter search to make sure it is returning what we requested.
 *
 * @author Glenn Renfro
 */

public class TwitterSearchTest extends AbstractIntegrationTest {

	@Test
	public void twitterSearchTest() throws Exception {

		final String data = "#springio";
		TwitterSearchSource twitterSearchSource = sources.twitterSearch(data, xdEnvironment.getTwitterConsumerKey(),
				xdEnvironment.getTwitterConsumerSecretKey());
		stream(twitterSearchSource + XD_DELIMETER + sinks.file());
		this.waitForXD();
		assertReceived("file.1", 1);
		assertContains(data);

	}
}
