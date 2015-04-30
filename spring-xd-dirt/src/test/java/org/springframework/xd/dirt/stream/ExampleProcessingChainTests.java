/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.xd.dirt.stream;

import static org.springframework.xd.dirt.test.process.SingleNodeProcessingChainSupport.chain;
import static org.springframework.xd.dirt.test.process.SingleNodeProcessingChainSupport.chainConsumer;
import static org.springframework.xd.dirt.test.process.SingleNodeProcessingChainSupport.chainProducer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.server.singlenode.SingleNodeApplication;
import org.springframework.xd.dirt.server.TestApplicationBootstrap;
import org.springframework.xd.dirt.test.SingleNodeIntegrationTestSupport;
import org.springframework.xd.dirt.test.process.SingleNodeProcessingChain;
import org.springframework.xd.dirt.test.process.SingleNodeProcessingChainConsumer;
import org.springframework.xd.dirt.test.process.SingleNodeProcessingChainProducer;
import org.springframework.xd.dirt.test.sink.NamedChannelSink;
import org.springframework.xd.dirt.test.sink.SingleNodeNamedChannelSinkFactory;
import org.springframework.xd.dirt.test.source.NamedChannelSource;
import org.springframework.xd.dirt.test.source.SingleNodeNamedChannelSourceFactory;

/**
 * What a Module/Stream developer might write to test a processing chain.
 * 
 * @author David Turanski
 */
public class ExampleProcessingChainTests {

	private static SingleNodeApplication application;

	private static SingleNodeIntegrationTestSupport integrationSupport;

	private static int RECEIVE_TIMEOUT = 5000;

	@BeforeClass
	public static void setUp() {
		// Args not required. Just shown as an example.
		// Also, used to pick an unused port
		application = new TestApplicationBootstrap().getSingleNodeApplication().run("--transport", "local");
		integrationSupport = new SingleNodeIntegrationTestSupport(application);
	}

	@Test
	public void testProcessingChain() {
		String processingChainUnderTest = "transform --expression='payload.toUpperCase()' | filter --expression='payload.length() > 4'";
		String streamDefinition = "queue:producer >" + processingChainUnderTest + "> queue:consumer";
		String streamName = "test";

		StreamDefinition testStream = new StreamDefinition(streamName, streamDefinition);
		integrationSupport.createAndDeployStream(testStream);

		MessageBus messageBus = integrationSupport.messageBus();

		NamedChannelSource source = new SingleNodeNamedChannelSourceFactory(messageBus).createNamedChannelSource("queue:producer");
		NamedChannelSink sink = new SingleNodeNamedChannelSinkFactory(messageBus).createNamedChannelSink("queue:consumer");

		source.sendPayload("hello");
		String result = (String) sink.receivePayload(RECEIVE_TIMEOUT);
		assertEquals("HELLO", result);

		source.sendPayload("a");
		result = (String) sink.receivePayload(RECEIVE_TIMEOUT);
		assertNull(result);

		source.unbind();
		sink.unbind();

		assertTrue("stream " + testStream.getName() + "not undeployed",
				integrationSupport.undeployAndDestroyStream(testStream));
	}

	/**
	 * Test a simple processing chain by sending and receiving payloads.
	 */
	@Test
	public void processingChain() {
		String processingChainUnderTest = "transform --expression='payload.toUpperCase()' | filter --expression='payload.length() > 4'";

		SingleNodeProcessingChain chain = chain(application, "eezypeasy", processingChainUnderTest);

		chain.sendPayload("hello");
		String result = (String) chain.receivePayload(RECEIVE_TIMEOUT);
		assertEquals("HELLO", result);

		chain.sendPayload("a");
		result = (String) chain.receivePayload(RECEIVE_TIMEOUT);
		assertNull(result);

		chain.destroy();
	}

	/**
	 * Test a processing chain that provides a source. Verify the result.
	 */
	@Test
	public void chainWithSource() {

		String processingChainUnderTest = "time | transform --expression='T(org.joda.time.format.DateTimeFormat).forPattern(\"yyyy-MM-dd HH:mm:ss\").parseDateTime(payload)'";

		SingleNodeProcessingChainConsumer chain = chainConsumer(application, "dateToDateTime", processingChainUnderTest);

		Object payload = chain.receivePayload(RECEIVE_TIMEOUT);
		assertTrue(payload instanceof DateTime);

		chain.destroy();
	}

	/**
	 * Test a processing chain that provides a sink. Send a payload and set up a tap to verify the result;
	 */
	@Test
	public void chainWithSink() {

		String processingChainUnderTest = "transform --expression='T(org.joda.time.format.DateTimeFormat).forPattern(\"yyyy-MM-dd HH:mm:ss\").parseDateTime(payload)' | log";

		SingleNodeProcessingChainProducer chain = chainProducer(application, "dateToDateTime", processingChainUnderTest);

		StreamDefinition tap = new StreamDefinition("testtap", "tap:stream:dateToDateTime.0 > queue:tap");
		integrationSupport.createAndDeployStream(tap);
		NamedChannelSink sink = new SingleNodeNamedChannelSinkFactory(integrationSupport.messageBus()).createNamedChannelSink("queue:tap");

		chain.sendPayload(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

		Object payload = sink.receivePayload(RECEIVE_TIMEOUT);
		assertNotNull(payload);
		assertTrue(payload instanceof DateTime);

		integrationSupport.undeployAndDestroyStream(tap);
		chain.destroy();
	}

	@AfterClass
	public static void tearDown() {
		if (application != null) {
			application.close();
		}
	}
}
