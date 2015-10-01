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

import java.util.UUID;

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

		final String streamName = "ts" + UUID.randomUUID().toString();
		final String data = "#katyperry";
		TwitterSearchSource twitterSearchSource = sources.twitterSearch(data);
		stream(streamName, twitterSearchSource + XD_DELIMITER + " file");
		this.waitForXD();
		assertFileContainsIgnoreCase(data, streamName);

	}
}
