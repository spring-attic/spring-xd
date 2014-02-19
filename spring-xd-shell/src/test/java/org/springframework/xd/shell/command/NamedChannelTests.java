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

import static org.junit.Assert.assertThat;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasValue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import org.springframework.xd.shell.command.fixtures.CounterSink;
import org.springframework.xd.shell.command.fixtures.HttpSource;

/**
 * Tests for named channels.
 * 
 * @author Ilayaperumal Gopinathan
 * @author Eric Bottard
 * @author Andy Clement
 */
public class NamedChannelTests extends AbstractStreamIntegrationTest {

	private static final Log logger = LogFactory.getLog(StreamCommandTests.class);

	@Test
	public void testCreateNamedChannelAsSink() {
		logger.info("Creating stream with named channel 'queue:foo' as sink");
		HttpSource source = newHttpSource();

		stream().create(generateStreamName(),
				"%s | transform --expression=payload.toUpperCase() > queue:foo", source);
	}

	@Test
	public void testCreateNamedChannelAsSource() throws InterruptedException {
		logger.info("Creating stream with named channel 'foo' as source");
		String streamName1 = generateStreamName();
		String streamName2 = generateStreamName();

		HttpSource httpSource = newHttpSource();
		CounterSink counterSink = metrics().newCounterSink();

		stream().create(streamName1, "%s | transform --expression=payload.toUpperCase() > queue:foo",
				httpSource);
		// Create stream with named channel as source
		stream().create(streamName2, "queue:foo > %s", counterSink);
		httpSource.ensureReady().postData("test");
		assertThat(counterSink, eventually(hasValue("1")));
	}

}
