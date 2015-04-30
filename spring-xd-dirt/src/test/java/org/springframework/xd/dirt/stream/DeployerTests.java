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

package org.springframework.xd.dirt.stream;

import static org.hamcrest.Matchers.containsString;

import java.util.Collections;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.xd.dirt.server.singlenode.SingleNodeApplication;
import org.springframework.xd.dirt.server.TestApplicationBootstrap;
import org.springframework.xd.dirt.test.SingleNodeIntegrationTestSupport;


/**
 * Test class to validate the deployment behavior that need not be tested across transports.  
 * 
 * @author Eric Bottard
 */
public class DeployerTests {

	private static SingleNodeApplication application;

	private static SingleNodeIntegrationTestSupport integrationSupport;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@BeforeClass
	public static void setUp() {
		application = new TestApplicationBootstrap().getSingleNodeApplication().run();
		integrationSupport = new SingleNodeIntegrationTestSupport(application);
	}

	@AfterClass
	public static void tearDown() {
		if (application != null) {
			application.close();
		}
	}

	@After
	public void cleanStreams() {
		integrationSupport.streamDeployer().deleteAll();
	}

	@Test
	public void testValidDeploymentProperties() {
		StreamDefinition testStream = new StreamDefinition("foo", "http | log");
		integrationSupport.streamDeployer().save(testStream);

		Map<String, String> properties = Collections.singletonMap("module.log.count", "3");
		integrationSupport.deployStream(testStream, properties);
	}

	@Test
	public void testValidDeploymentPropertiesUsesLabel() {
		StreamDefinition testStream = new StreamDefinition("foo", "http | bar:log");
		integrationSupport.streamDeployer().save(testStream);

		Map<String, String> properties = Collections.singletonMap("module.bar.count", "3");
		integrationSupport.deployStream(testStream, properties);
	}

	@Test
	public void testInvalidDeploymentProperties() {
		StreamDefinition testStream = new StreamDefinition("foo", "http | log");
		integrationSupport.streamDeployer().save(testStream);

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage(containsString("module.logo.count"));

		Map<String, String> properties = Collections.singletonMap("module.logo.count", "3");
		integrationSupport.deployStream(testStream, properties);
	}

	@Test
	public void testInvalidDeploymentPropertiesShouldUseLabel() {
		StreamDefinition testStream = new StreamDefinition("foo", "http | bar: log");
		integrationSupport.streamDeployer().save(testStream);

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage(containsString("module.log.count"));

		Map<String, String> properties = Collections.singletonMap("module.log.count", "3");
		integrationSupport.deployStream(testStream, properties);
	}


}
