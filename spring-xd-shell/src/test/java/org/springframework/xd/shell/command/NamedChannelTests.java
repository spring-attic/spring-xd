/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.shell.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import org.springframework.xd.shell.command.fixtures.HttpSource;

/**
 * Tests for named channels.
 * 
 * @author Ilayaperumal Gopinathan
 * 
 */
public class NamedChannelTests extends AbstractStreamIntegrationTest {

	private static final Log logger = LogFactory.getLog(StreamCommandTests.class);

	@Test
	public void testCreateNamedChannelAsSink() {
		logger.info("Creating stream with named channel 'foo' as sink");
		stream().create("namedchanneltest-ticktock",
				"http --port=%s | transform --expression=payload.toUpperCase() > :foo", DEFAULT_HTTP_PORT);
	}

	@Test
	public void testCreateNamedChannelAsSource() throws InterruptedException {
		logger.info("Creating stream with named channel 'foo' as source");
		String stream1 = "namedchanneltest-ticktock";
		String stream2 = "namedchanneltest-ticktock-counter";

		HttpSource httpSource = newHttpSource();

		stream().create(stream1, "%s | transform --expression=payload.toUpperCase() > :foo",
				httpSource);
		// Create stream with named channel as source
		stream().create(stream2, ":foo > counter --name=%s", DEFAULT_METRIC_NAME);
		httpSource.ensureReady().postData("test");
		counter().verifyCounter("1");
	}

}
