/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.x.bus.MessageBus;
import org.springframework.xd.dirt.integration.test.SingleNodeIntegrationTestSupport;
import org.springframework.xd.dirt.integration.test.sink.NamedChannelSink;
import org.springframework.xd.dirt.integration.test.sink.SingleNodeNamedChannelSinkFactory;
import org.springframework.xd.dirt.integration.test.source.NamedChannelSource;
import org.springframework.xd.dirt.integration.test.source.SingleNodeNamedChannelSourceFactory;
import org.springframework.xd.dirt.server.SingleNodeApplication;
import org.springframework.xd.dirt.server.TestApplicationBootstrap;
import org.springframework.xd.module.options.EnvironmentAwareModuleOptionsMetadataResolver;


/**
 * Tests that ensure that defaults computed by {@link EnvironmentAwareModuleOptionsMetadataResolver} come in the correct
 * order, namely
 * <ol>
 * <li>inline DSL option in the stream definition</li>
 * <li>System properties</li>
 * <li>Environment variables (not tested, but assimilated to the above)</li>
 * <li>the "xd-config.yml" file (driven by {@code spring.config.location} property)
 * <li>application.yml file (tested here by providing a different name with {@code -Dspring.config.name})</li>
 * <li>the actual module default</li>
 * </ol>
 */
public class ModuleOptionsDefaultsOrderingTests {

	private Properties previousSystemProp;

	private String dslDefinition = "transform ";

	@Test
	public void testStreamDefinitionComes1st() {
		dslDefinition += " --expression='''dsl'''";
		System.setProperty("processor.transform.expression", "'systemprop'");
		System.setProperty("spring.config.location", "classpath:/ModuleOptionsDefaultsOrderingTests-xd-config.yml");
		System.setProperty("spring.config.name", "application-test");

		runTest("dsl");
	}


	@Test
	public void testSystemPropsAndEnvCome2nd() {
		// dslDefinition += " --expression='''dsl'''";
		System.setProperty("processor.transform.expression", "'systemprop'");
		System.setProperty("spring.config.location", "classpath:/ModuleOptionsDefaultsOrderingTests-xd-config.yml");
		System.setProperty("spring.config.name", "application-test");
		runTest("systemprop");

	}

	@Test
	public void testXdConfigComes3rd() {
		// dslDefinition += " --expression='''dsl'''";
		// System.setProperty("processor.transform.expression", "'systemprop'");
		System.setProperty("spring.config.location", "classpath:/ModuleOptionsDefaultsOrderingTests-xd-config.yml");
		System.setProperty("spring.config.name", "application-test");
		runTest("xd-config-value");
	}

	@Test
	public void testApplicationCome4th() {
		// dslDefinition += " --expression='''dsl'''";
		// System.setProperty("processor.transform.expression", "'systemprop'");
		// System.setProperty("spring.config.location", "classpath:/ModuleOptionsDefaultsOrderingTests-xd-config.yml");
		System.setProperty("spring.config.name", "application-test");
		runTest("application-value");
	}

	@Test
	public void testModuleDefaultsCome5th() {
		// dslDefinition += " --expression='''dsl'''";
		// System.setProperty("processor.transform.expression", "'systemprop'");
		// System.setProperty("spring.config.location", "classpath:/ModuleOptionsDefaultsOrderingTests-xd-config.yml");
		// System.setProperty("spring.config.name", "application-test");
		runTest("ping");
	}


	@Before
	public void rememberSystemProps() {
		// Make a copy!
		previousSystemProp = new Properties();
		previousSystemProp.putAll(System.getProperties());
	}

	/**
	 * Make sure to cleanup after ourselves as some of the stuff is per-JVM
	 */
	@After
	public void cleanup() {
		System.setProperties(previousSystemProp);
	}

	private void runTest(String expected) {
		SingleNodeApplication application = new TestApplicationBootstrap().getSingleNodeApplication().run(
				"--transport", "local");
		SingleNodeIntegrationTestSupport integrationSupport = new SingleNodeIntegrationTestSupport(application);

		String streamDefinition = String.format("queue:producer > %s > queue:consumer", dslDefinition);
		String streamName = "test";

		StreamDefinition testStream = new StreamDefinition(streamName, streamDefinition);
		integrationSupport.createAndDeployStream(testStream);

		MessageBus messageBus = integrationSupport.messageBus();

		NamedChannelSource source = new SingleNodeNamedChannelSourceFactory(messageBus).createNamedChannelSource("queue:producer");
		NamedChannelSink sink = new SingleNodeNamedChannelSinkFactory(messageBus).createNamedChannelSink("queue:consumer");

		source.sendPayload("ping");
		String result = (String) sink.receivePayload(5000);
		assertEquals(expected, result);

		source.unbind();
		sink.unbind();

		assertTrue("stream " + testStream.getName() + "not undeployed",
				integrationSupport.undeployAndDestroyStream(testStream));
	}
}
