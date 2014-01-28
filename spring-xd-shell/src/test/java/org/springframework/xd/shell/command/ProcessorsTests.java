/*
 * Copyright 2013 the original author or authors.
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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasContentsThat;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasValue;

import java.io.IOException;

import org.junit.Test;

import org.springframework.xd.shell.command.fixtures.CounterSink;
import org.springframework.xd.shell.command.fixtures.FileSink;
import org.springframework.xd.shell.command.fixtures.HttpSource;


/**
 * Shell integration tests for various simple processor modules.
 * 
 * @author Eric Bottard
 */
public class ProcessorsTests extends AbstractStreamIntegrationTest {

	@Test
	public void splitterDoesNotSplitByDefault() throws Exception {
		HttpSource httpSource = newHttpSource();
		CounterSink counterSink = metrics().newCounterSink();

		stream().create(generateStreamName(), "%s | splitter | %s", httpSource, counterSink);

		httpSource.ensureReady().postData("Hello World !");
		assertThat(counterSink, eventually(hasValue("1")));

	}

	@Test
	public void splitterDoesSplit() {
		HttpSource httpSource = newHttpSource();
		CounterSink counterSink = metrics().newCounterSink();

		stream().create(generateStreamName(), "%s | splitter --expression=payload.split(' ') | %s",
				httpSource, counterSink);

		httpSource.ensureReady().postData("Hello World !");
		assertThat(counterSink, eventually(hasValue("3")));

	}

	@Test
	public void testAggregatorNormalRelease() throws IOException {
		HttpSource httpSource = newHttpSource();
		FileSink fileSink = newFileSink().binary(true);

		stream().create(
				generateStreamName(),
				"%s | aggregator --count=3 --aggregation=T(org.springframework.util.StringUtils).collectionToDelimitedString(#this.![payload],' ') | %s",
				httpSource, fileSink);

		httpSource.ensureReady().postData("Hello").postData("World").postData("!");

		assertThat(fileSink, eventually(hasContentsThat(equalTo("Hello World !"))));

	}

	@Test
	public void testAggregatorEarlyRelease() throws IOException {
		HttpSource httpSource = newHttpSource();
		FileSink fileSink = newFileSink().binary(true);

		int timeout = 1000;

		stream().create(
				generateStreamName(),
				"%s | aggregator --count=100 --timeout=%d --aggregation=T(org.springframework.util.StringUtils).collectionToDelimitedString(#this.![payload],' ') | %s",
				httpSource, timeout, fileSink);

		httpSource.ensureReady().postData("Hello").postData("World").postData("!");

		// The reaper and the task scheduler are both configured with 'timeout'
		// so in the worst case, it can take 2*timeout to actually flush the msgs
		assertThat(fileSink, eventually(1, (int) (2.1 * timeout), hasContentsThat(equalTo("Hello World !"))));

	}
}
