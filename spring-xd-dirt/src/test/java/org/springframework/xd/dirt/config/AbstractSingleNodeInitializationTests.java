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

package org.springframework.xd.dirt.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.module.ModuleDeployer;
import org.springframework.xd.dirt.server.SingleNodeApplication;
import org.springframework.xd.module.core.PluginAdapter;


/**
 * 
 * @author David Turanski
 */
public abstract class AbstractSingleNodeInitializationTests {

	protected AbstractApplicationContext context;

	protected AbstractApplicationContext moduleContext;

	protected ModuleDeployer moduleDeployer;

	private SingleNodeApplication application;

	@Before
	public final void setUp() {
		this.application = new SingleNodeApplication();

		String[] args = {};
		args = addArgIfProvided(args, "transport", getTransport());
		args = addArgIfProvided(args, "controlTransport", getControlTransport());
		application.run(args);

		this.context = (AbstractApplicationContext) application.getContainerContext();

		this.moduleDeployer = this.context.getBean(ModuleDeployer.class);

		ConfigurableApplicationContext deployerContext = TestUtils.getPropertyValue(this.moduleDeployer,
				"deployerContext",
				ConfigurableApplicationContext.class);
		deployerContext.getBeanFactory().registerSingleton("testPlugin", new TestPlugin());

		moduleDeployer.onInit();

		setupApplicationContext(this.context);
	}

	protected void setupApplicationContext(ApplicationContext context) {
	}

	protected abstract void cleanup(ApplicationContext context);

	protected abstract String getTransport();

	protected abstract Class<? extends MessageBus> getExpectedMessageBusType();

	protected abstract MessageChannel getControlChannel();

	@After
	public final void shutDown() {
		this.cleanup(this.context);
		this.application.close();
	}

	@Test
	public final void environmentMatchesTransport() {
		MessageChannel controlChannel = this.context.getBean("containerControlChannel", MessageChannel.class);
		assertSame(controlChannel, getControlChannel());
		MessageBus messageBus = this.moduleContext.getBean(MessageBus.class);
		assertEquals(getExpectedMessageBusType(), messageBus.getClass());
	}

	public class TestPlugin extends PluginAdapter {

		@Override
		public void preProcessSharedContext(ConfigurableApplicationContext context) {
			moduleContext = (AbstractApplicationContext) context;
		}
	}

	private String[] addArgIfProvided(String[] args, String argName, String argVal) {
		if (StringUtils.hasText(argVal)) {
			args = Arrays.copyOf(args, args.length + 2);
			args[args.length - 2] = "--" + argName;
			args[args.length - 1] = argVal;
		}
		return args;
	}

	protected String getControlTransport() {
		return null;
	}
}
