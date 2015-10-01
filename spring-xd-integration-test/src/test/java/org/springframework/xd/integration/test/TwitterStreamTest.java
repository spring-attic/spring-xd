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


/**
 * Test to verify that data is being received from the twitterstream source.
 *
 * @author Glenn Renfro
 */

public class TwitterStreamTest extends AbstractIntegrationTest {

	@Test
	public void twitterStream() throws Exception {
		final String streamName = "ts" + UUID.randomUUID().toString();
		final String data = "screen_name";
		stream(streamName, sources.twitterStream()
				+ XD_DELIMITER
				+ sinks.file());
		this.waitForXD();
		assertFileContains(data, streamName);
	}
}
