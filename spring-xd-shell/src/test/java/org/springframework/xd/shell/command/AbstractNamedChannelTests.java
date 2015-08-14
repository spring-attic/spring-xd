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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.xd.shell.command.fixtures.HttpSource;
import org.springframework.xd.test.fixtures.CounterSink;

/**
 * Tests for named channels.
 * 
 * @author Ilayaperumal Gopinathan
 * @author Eric Bottard
 * @author Andy Clement
 */
public class AbstractNamedChannelTests extends AbstractStreamIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(AbstractStreamCommandsTests.class);

	@Test
	public void testCreateNamedChannelAsSink() {
		String streamName = generateStreamName();
		String queueName = generateQueueName();
		logger.info("Creating stream with named channel " + queueName + " as sink");
		HttpSource source = newHttpSource();

		stream().create(streamName,
				"%s | transform --expression=payload.toUpperCase() > %s", source, queueName);
	}

	@Test
	public void testCreateNamedChannelAsSource() throws InterruptedException {
		String streamName1 = generateStreamName();
		String streamName2 = generateStreamName();
		String queueName = generateQueueName();
		logger.info("Creating stream with named channel '" + queueName + "' as source");
		HttpSource httpSource = newHttpSource();
		CounterSink counterSink = metrics().newCounterSink();

		stream().create(streamName1, "%s | transform --expression=payload.toUpperCase() > %s",
				httpSource, queueName);
		// Create stream with named channel as source
		stream().create(streamName2, "%s > %s", queueName, counterSink);
		httpSource.ensureReady().postData("test");
		assertThat(counterSink, eventually(hasValue("1")));
	}
}
