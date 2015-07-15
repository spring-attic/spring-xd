/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.xd.spark.streaming;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.eventually;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.exists;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.fileContent;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasContentsThat;
import static org.springframework.xd.shell.command.fixtures.XDMatchers.hasValue;

import java.io.File;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import org.springframework.shell.Bootstrap;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.xd.dirt.module.ResourceModuleRegistry;
import org.springframework.xd.dirt.server.singlenode.SingleNodeApplication;
import org.springframework.xd.dirt.test.SingleNodeIntegrationTestSupport;
import org.springframework.xd.shell.command.MetricsTemplate;
import org.springframework.xd.shell.command.StreamCommandsTemplate;
import org.springframework.xd.shell.command.fixtures.HttpSource;
import org.springframework.xd.test.RandomConfigurationSupport;
import org.springframework.xd.test.fixtures.CounterSink;
import org.springframework.xd.test.fixtures.FileSink;


/**
 * Abstract Spark streaming test class which can be extended to run against multiple XD transport.
 *
 * @author Ilayaperumal Gopinathan
 */
public abstract class AbstractSparkStreamingTests {

	private static final String TEST_MESSAGE = "foo foo foo";

	protected static final String TEST_LONG_MESSAGE = "foo foo foo foo bar bar bar foo bar bar test1 test1 test2 foo";

	private SingleNodeApplication singleNodeApplication;

	private SingleNodeIntegrationTestSupport integrationTestSupport;

	private JLineShellComponent shell;

	protected StreamCommandsTemplate streamOps;

	private MetricsTemplate metrics;

	private final String transport;

	public AbstractSparkStreamingTests(String transport) {
		this.transport = transport;
	}

	@Rule
	public TestName testName = new TestName();

	@Before
	public void setup() throws Exception {
		RandomConfigurationSupport randomConfigSupport = new RandomConfigurationSupport();
		singleNodeApplication = new SingleNodeApplication().run("--transport", this.transport);
		integrationTestSupport = new SingleNodeIntegrationTestSupport(singleNodeApplication);
		integrationTestSupport.addModuleRegistry(new ResourceModuleRegistry("classpath:/spring-xd/xd/modules"));
		Bootstrap bootstrap = new Bootstrap(new String[] {"--port", randomConfigSupport.getAdminServerPort()});
		shell = bootstrap.getJLineShellComponent();
		if (!shell.isRunning()) {
			shell.start();
		}
		streamOps = new StreamCommandsTemplate(shell, integrationTestSupport);
		metrics = new MetricsTemplate(shell);
	}

	@After
	public void tearDown() {
		if (singleNodeApplication != null) {
			singleNodeApplication.close();
		}
		singleNodeApplication = null;
		if (shell != null) {
			shell.stop();
		}
		shell = null;
	}

	protected void createStream(String streamName, String stream) {
		streamOps.create(streamName, stream);
	}

	@Test
	public void testSparkProcessor() throws Exception {
		HttpSource source = new HttpSource(shell);
		String streamName =  testName.getMethodName() + new Random().nextInt();
		FileSink sink = new FileSink().binary(true);
		try {
			String stream = String.format("%s | spark-word-count | %s --inputType=text/plain", source, sink);
			createStream(streamName, stream);
			source.ensureReady().postData(TEST_LONG_MESSAGE);
			assertThat(sink, eventually(hasContentsThat(containsString("(foo,6)"))));
			assertThat(sink, eventually(hasContentsThat(containsString("(bar,5)"))));
			assertThat(sink, eventually(hasContentsThat(containsString("(test1,2)"))));
			assertThat(sink, eventually(hasContentsThat(containsString("(test2,1)"))));
		}
		finally {
			streamOps.destroyStream(streamName);
			sink.cleanup();
		}
	}

	@Test
	public void testSparkTapStream() throws Exception {
		HttpSource source = new HttpSource(shell);
		String streamName =  testName.getMethodName() + new Random().nextInt();
		String tapStreamName =  testName.getMethodName() + new Random().nextInt();
		FileSink sink = new FileSink().binary(true);
		try {
			String stream = String.format("%s | counter", source);
			createStream(streamName, stream);
			String tapStream = String.format("tap:stream:%s > spark-word-count | %s --inputType=text/plain",
					streamName, sink);
			createStream(tapStreamName, tapStream);
			source.ensureReady().postData(TEST_LONG_MESSAGE);
			assertThat(sink, eventually(hasContentsThat(containsString("(foo,6)"))));
			assertThat(sink, eventually(hasContentsThat(containsString("(bar,5)"))));
			assertThat(sink, eventually(hasContentsThat(containsString("(test1,2)"))));
			assertThat(sink, eventually(hasContentsThat(containsString("(test2,1)"))));
		}
		finally {
			streamOps.destroyStream(streamName);
			streamOps.destroyStream(tapStreamName);
			sink.cleanup();
		}
	}

	@Test
	public void testTapSparkProcessor() throws Exception {
		HttpSource source = new HttpSource(shell);
		String streamName =  testName.getMethodName() + new Random().nextInt();
		String tapStreamName =  testName.getMethodName() + new Random().nextInt();
		CounterSink counter = metrics.newCounterSink();
		try {
			String stream = String.format("%s | spark-word-count --enableTap=true | null", source);
			createStream(streamName, stream);
			String tapStream = String.format("tap:stream:%s.spark-word-count > %s --inputType=text/plain", streamName, counter);
			createStream(tapStreamName, tapStream);
			source.ensureReady().postData(TEST_LONG_MESSAGE);
			assertThat(counter, eventually(50, 100, exists()));
			assertThat(counter, eventually(hasValue("4")));
		}
		finally {
			streamOps.destroyStream(streamName);
			streamOps.destroyStream(tapStreamName);
			counter.cleanup();
		}
	}

	@Test
	public void testSparkProcessorWithInputType() throws Exception {
		HttpSource source = new HttpSource(shell);
		String streamName =  testName.getMethodName()  + new Random().nextInt();
		FileSink sink = new FileSink().binary(true);
		try {
			String stream = String.format("%s | spark-word-count --inputType=text/plain | %s " +
					"--inputType=text/plain", source, sink);
			createStream(streamName, stream);
			source.ensureReady().postData(TEST_MESSAGE);
			assertThat(sink, eventually(hasContentsThat(equalTo("(foo,3)"))));
		}
		finally {
			streamOps.destroyStream(streamName);
			sink.cleanup();
		}
	}

	@Test
	public void testSparkProcessorWithOutputType() throws Exception {
		HttpSource source = new HttpSource(shell);
		String streamName =  testName.getMethodName()  + new Random().nextInt();
		FileSink sink = new FileSink().binary(true);
		try {
			String stream = String.format("%s | spark-word-count --outputType=application/json | %s", source, sink);
			createStream(streamName, stream);
			source.ensureReady().postData(TEST_MESSAGE);
			assertThat(sink, eventually(hasContentsThat(equalTo("{\"_1\":\"foo\",\"_2\":3}"))));
		}
		finally {
			streamOps.destroyStream(streamName);
			sink.cleanup();
		}
	}

	@Test
	public void testSparkScalaProcessor() throws Exception {
		HttpSource source = new HttpSource(shell);
		String streamName =  testName.getMethodName()  + new Random().nextInt();
		FileSink sink = new FileSink().binary(true);
		try {
			String stream = String.format("%s | spark-scala-word-count | %s --inputType=text/plain", source, sink);
			createStream(streamName, stream);
			source.ensureReady().postData(TEST_LONG_MESSAGE);
			assertThat(sink, eventually(hasContentsThat(containsString("(foo,6)"))));
			assertThat(sink, eventually(hasContentsThat(containsString("(bar,5)"))));
			assertThat(sink, eventually(hasContentsThat(containsString("(test1,2)"))));
			assertThat(sink, eventually(hasContentsThat(containsString("(test2,1)"))));
		}
		finally {
			streamOps.destroyStream(streamName);
			sink.cleanup();
		}
	}

	@Test
	public void testSparkScalaProcessorWithInputType() throws Exception {
		HttpSource source = new HttpSource(shell);
		String streamName =  testName.getMethodName()  + new Random().nextInt();
		FileSink sink = new FileSink().binary(true);
		try {
			String stream = String.format("%s | spark-scala-word-count --inputType=text/plain | " +
					"%s --inputType=text/plain", source, sink);
			createStream(streamName, stream);
			source.ensureReady().postData(TEST_MESSAGE);
			assertThat(sink, eventually(hasContentsThat(equalTo("(foo,3)"))));
		}
		finally {
			streamOps.destroyStream(streamName);
			sink.cleanup();
		}
	}

	@Test
	public void testSparkScalaProcessorWithOutputType() throws Exception {
		HttpSource source = new HttpSource(shell);
		String streamName =  testName.getMethodName()  + new Random().nextInt();
		FileSink sink = new FileSink().binary(true);
		try {
			String stream = String.format("%s | spark-scala-word-count --outputType=application/json | %s", source, sink);
			createStream(streamName, stream);
			source.ensureReady().postData(TEST_MESSAGE);
			assertThat(sink, eventually(hasContentsThat(equalTo("{\"_1\":\"foo\",\"_2\":3}"))));
		}
		finally {
			streamOps.destroyStream(streamName);
			sink.cleanup();
		}
	}

	@Test
	public void testSparkFileLogger() throws Exception {
		String streamName = testName.getMethodName()  + new Random().nextInt();
		String fileName = streamName + ".txt";
		File file = new File(fileName);
		try {
			final HttpSource source = new HttpSource(shell);
			final String stream = String.format("%s | file-logger --path=%s", source, fileName);
			createStream(streamName, stream);
			source.ensureReady().postData(TEST_MESSAGE);
			assertThat(file, eventually(50, 100, fileContent(endsWith(TEST_MESSAGE + System.lineSeparator()))));
		}
		finally {
			streamOps.destroyStream(streamName);
			if (file.exists()) {
				file.delete();
			}
		}
	}

	@Test
	public void testSparkFileLoggerWithInputType() throws Exception {
		String streamName = testName.getMethodName() + new Random().nextInt();
		String fileName = streamName + ".txt";
		File file = new File(fileName);
		try {
			final HttpSource source = new HttpSource(shell);
			final String stream = String.format("%s | file-logger --path=%s --inputType=text/plain", source, fileName);
			createStream(streamName, stream);
			source.ensureReady().postData(TEST_MESSAGE);
			assertThat(file, eventually(50, 100, fileContent(endsWith(TEST_MESSAGE + System.lineSeparator()))));
		}
		finally {
			streamOps.destroyStream(streamName);
			if (file.exists()) {
				file.delete();
			}
		}
	}

	@Test
	public void testSparkScalaFileLogger() throws Exception {
		String streamName = testName.getMethodName()  + new Random().nextInt();
		String fileName = streamName + ".txt";
		File file = new File(fileName);
		try {
			final HttpSource source = new HttpSource(shell);
			final String stream = String.format("%s | file-logger-scala --path=%s", source, fileName);
			createStream(streamName, stream);
			source.ensureReady().postData(TEST_MESSAGE);
			assertThat(file, eventually(50, 100, fileContent(endsWith(TEST_MESSAGE + System.lineSeparator()))));
		}
		finally {
			streamOps.destroyStream(streamName);
			if (file.exists()) {
				file.delete();
			}
		}
	}

	@Test
	public void testSparkScalaFileLoggerWithInputType() throws Exception {
		String streamName = testName.getMethodName()  + new Random().nextInt();
		String fileName = streamName + ".txt";
		File file = new File(fileName);
		try {
			final HttpSource source = new HttpSource(shell);
			final String stream = String.format("%s | file-logger-scala --path=%s --inputType=text/plain", source, fileName);
			createStream(streamName, stream);
			source.ensureReady().postData(TEST_MESSAGE);
			assertThat(file, eventually(50, 100, fileContent(endsWith(TEST_MESSAGE + System.lineSeparator()))));
		}
		finally {
			streamOps.destroyStream(streamName);
			if (file.exists()) {
				file.delete();
			}
		}
	}

}
