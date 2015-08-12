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

import org.springframework.xd.shell.command.fixtures.HttpSource;
import org.springframework.xd.test.fixtures.CounterSink;
import org.springframework.xd.test.fixtures.FileSink;


/**
 * Shell integration tests for various simple processor modules.
 *
 * @author Eric Bottard
 */
public class AbstractProcessorsTests extends AbstractStreamIntegrationTest {

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

		// Although the reaper and the task scheduler are both configured with 'timeout',
		// this does not take into account the amount of time it may take to deploy the
		// stream in a distributed environment; therefore this assert will use the default
		// timeouts configured in the eventually/hasContentsThat methods.
		assertThat(fileSink, eventually(hasContentsThat(equalTo("Hello World !"))));

	}

	@Test
	public void testAggregatorCorrelation() throws IOException {
		HttpSource httpSource = newHttpSource();
		FileSink fileSink = newFileSink().binary(true);

		stream().create(
				generateStreamName(),
				"%s | aggregator --count=3 --aggregation=T(org.springframework.util.StringUtils).collectionToDelimitedString(#this.![payload],' ') "
						+ "--correlation=payload.length() | %s",
				httpSource, fileSink);

		httpSource.ensureReady().postData("Hello").postData("World").postData("!");
		httpSource.ensureReady().postData("I").postData("am").postData("1");
		httpSource.ensureReady().postData("tu").postData("es").postData("fifth");

		String expected = "! I 1";
		expected += "am tu es";
		expected += "Hello World fifth";
		assertThat(fileSink, eventually(hasContentsThat(equalTo(expected))));

	}

	@Test
	public void testScriptTransformProcessor() {
		HttpSource httpSource = newHttpSource();
		FileSink fileSink = newFileSink().binary(true);
		stream().create(generateStreamName(),
				"%s | transform --script='org/springframework/xd/shell/command/transform-to-lowercase.groovy' | %s",
				httpSource, fileSink);
		httpSource.ensureReady().postData("HELLO").postData("World").postData("!");
		assertThat(fileSink, eventually(hasContentsThat(equalTo("helloworld!"))));
	}

	@Test
	public void testScriptTransformProcessorWithPropertiesLocation() {
		HttpSource httpSource = newHttpSource();
		FileSink fileSink = newFileSink().binary(true);
		stream().create(generateStreamName(),
				"%s | transform --script='org/springframework/xd/shell/command/script-with-variables.groovy' " +
						"--propertiesLocation='org/springframework/xd/shell/command/script.properties'" +
						" | %s",
				httpSource, fileSink);
		httpSource.ensureReady().postData("hello");
		assertThat(fileSink, eventually(hasContentsThat(equalTo("helloFOO"))));
	}

	@Test
	public void testScriptTransformProcessorWithInlineProperties() {
		HttpSource httpSource = newHttpSource();
		FileSink fileSink = newFileSink().binary(true);
		stream().create(generateStreamName(),
				"%s | transform --script='org/springframework/xd/shell/command/script-with-variables.groovy' " +
						"--variables='foo=FOO'" +
						" | %s",
				httpSource, fileSink);
		httpSource.ensureReady().postData("hello");
		assertThat(fileSink, eventually(hasContentsThat(equalTo("helloFOO"))));
	}

	@Test
	public void testScriptInlinePropertiesOverridesLocation() {
		HttpSource httpSource = newHttpSource();
		FileSink fileSink = newFileSink().binary(true);
		stream().create(generateStreamName(),
				"%s | transform --script='org/springframework/xd/shell/command/script-with-variables.groovy' " +
						"--variables='foo=BAR' --propertiesLocation='org/springframework/xd/shell/command/script.properties'" +
						" | %s",
				httpSource, fileSink);
		httpSource.ensureReady().postData("hello");
		assertThat(fileSink, eventually(hasContentsThat(equalTo("helloBAR"))));
	}

	@Test
	public void testExpressionTransformProcessor() {
		HttpSource httpSource = newHttpSource();
		FileSink fileSink = newFileSink().binary(true);
		stream().create(generateStreamName(),
				"%s | transform --expression=payload.toString().toUpperCase() | %s",
				httpSource, fileSink);
		httpSource.ensureReady().postData("hello").postData("World").postData("!");
		assertThat(fileSink, eventually(hasContentsThat(equalTo("HELLOWORLD!"))));
	}


}
