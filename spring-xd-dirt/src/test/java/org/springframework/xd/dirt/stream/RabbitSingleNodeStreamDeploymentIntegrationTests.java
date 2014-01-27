/*
 * Copyright 2013-2014 the original author or authors.
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

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.integration.x.bus.RabbitTestMessageBus;
import org.springframework.xd.test.rabbit.RabbitTestSupport;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class RabbitSingleNodeStreamDeploymentIntegrationTests extends
		AbstractSingleNodeStreamDeploymentIntegrationTests {

	@ClassRule
	public static RabbitTestSupport rabbitAvailableRule = new RabbitTestSupport();

	@BeforeClass
	public static void setUp() {
		setUp("rabbit");
	}

	@ClassRule
	public static ExternalResource initializeRabbitTestMessageBus = new ExternalResource() {

		@Override
		protected void before() {
			if (testMessageBus == null) {
				testMessageBus = new RabbitTestMessageBus(rabbitAvailableRule.getResource(), getCodec());
			}
		}
	};

	@AfterClass
	public static void cleanup() {
		if (context != null) {
			RabbitAdmin admin = context.getBean(RabbitAdmin.class);
			String deployerQueue = context.getEnvironment().resolvePlaceholders(XD_DEPLOYER_PLACEHOLDER);
			String undeployerExchange = context.getEnvironment().resolvePlaceholders(XD_UNDEPLOYER_PLACEHOLDER);
			CachingConnectionFactory ccf = context.getBean(CachingConnectionFactory.class);
			ccf.setConnectionListeners(new ArrayList<ConnectionListener>());
			admin.setApplicationContext(null);
			application.close();
			admin.deleteQueue(deployerQueue);
			admin.deleteExchange(undeployerExchange);
		}
	}
}
