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

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.server.singlenode.SingleNodeApplication;
import org.springframework.xd.dirt.server.TestApplicationBootstrap;
import org.springframework.xd.dirt.test.SingleNodeIntegrationTestSupport;
import org.springframework.xd.dirt.test.sink.NamedChannelSink;
import org.springframework.xd.dirt.test.sink.SingleNodeNamedChannelSinkFactory;
import org.springframework.xd.dirt.test.source.NamedChannelSource;
import org.springframework.xd.dirt.test.source.SingleNodeNamedChannelSourceFactory;
import org.springframework.xd.module.options.EnvironmentAwareModuleOptionsMetadataResolver;


/**
 * Tests that ensure that defaults computed by {@link EnvironmentAwareModuleOptionsMetadataResolver} come in the correct
 * order, namely
 * <ol>
 * <li>inline DSL option in the stream definition</li>
 * <li>System properties</li>
 * <li>Environment variables (not tested, but assimilated to the above)</li>
 * <li>values in the {@code <root>/<type>/<module>/<module>.properties} file</li>
 * <li>values in the {@code <root>/config/modules/modules.yml} file</li>
 * <li>the actual module default</li>
 * </ol>
 */
/*
 * Most of the test methods in this class work by incrementally peeling out layers of configuration and verifying that
 * the correct value is witnessed at runtime.
 */
public class ModuleOptionsDefaultsOrderingTests {

	private Properties previousSystemProp;

	private String dslDefinition = "transform ";

	private String activeProfiles = null;

	@Test
	public void testStreamDefinitionComes1st() {
		setValueInStreamDefinitionItself(true);
		setValueAsSystemProperty(true);
		setValueInModuleConfigFiles(true, "with-leaf");

		runTestAndExpect("dsl");
	}


	@Test
	public void testSystemPropsAndEnvCome2nd() {
		setValueInStreamDefinitionItself(false);
		setValueAsSystemProperty(true);
		setValueInModuleConfigFiles(true, "with-leaf");
		runTestAndExpect("systemprop");

	}


	@Test
	public void testXdModuleConfigLeavesComes3rd() {
		setValueInStreamDefinitionItself(false);
		setValueAsSystemProperty(false);
		setValueInModuleConfigFiles(true, "with-leaf");
		runTestAndExpect("value-from-properties-file");
	}

	@Test
	public void testXdModuleConfigRootComes4th() {
		setValueInStreamDefinitionItself(false);
		setValueAsSystemProperty(false);
		setValueInModuleConfigFiles(true, "without-leaf");
		runTestAndExpect("global-value");
	}

	@Test
	public void testModuleDefaultsCome5th() {
		setValueInStreamDefinitionItself(false);
		setValueAsSystemProperty(false);
		setValueInModuleConfigFiles(false, "with-leaf");
		runTestAndExpect("ping");
	}

	@Test
	public void testProfileVariationsAtLeafLevel() {
		activeProfiles = "big,prod";

		setValueInStreamDefinitionItself(false);
		setValueAsSystemProperty(false);
		setValueInModuleConfigFiles(true, "with-leaf");

		runTestAndExpect("value-from-properties-file-prod");
	}

	@Before
	public void rememberSystemProps() {
		// Make a copy!
		previousSystemProp = new Properties();
		previousSystemProp.putAll(System.getProperties());
	}

	/**
	 * Make sure to cleanup after ourselves as some of the stuff is per-JVM.
	 */
	@After
	public void cleanup() {
		System.setProperties(previousSystemProp);
	}

	private void runTestAndExpect(String expected) {
		SingleNodeApplication application = new TestApplicationBootstrap().getSingleNodeApplication().run(
				"--transport", "local");

		// Set activate profiles AFTER the container has started, so we don't
		// interfere with container profiles themselves
		if (activeProfiles != null) {
			System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, activeProfiles);
		}
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
		application.close();
	}


	private void setValueAsSystemProperty(boolean active) {
		if (active) {
			System.setProperty("processor.transform.expression", "'systemprop'");
		}
	}


	private void setValueInStreamDefinitionItself(boolean active) {
		if (active) {
			dslDefinition += " --expression='''dsl'''";
		}
	}


	private void setValueInModuleConfigFiles(boolean active, String version) {
		if (active) {
			System.setProperty("xd.module.config.location",
					"classpath:/ModuleOptionsDefaultsOrderingTests-module-config-" + version + "/");
			System.setProperty("xd.module.config.name", "test-xd-module-config");
		}
	}
}
