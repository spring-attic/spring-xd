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

package org.springframework.xd.dirt.config;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.module.ModuleDeployer;
import org.springframework.xd.dirt.server.singlenode.SingleNodeApplication;
import org.springframework.xd.dirt.server.TestApplicationBootstrap;

/**
 * @author David Turanski
 */
public abstract class AbstractSingleNodeInitializationTests {

	protected AbstractApplicationContext pluginContext;

	protected AbstractApplicationContext containerContext;

	protected AbstractApplicationContext adminContext;

	protected ModuleDeployer moduleDeployer;

	protected TestApplicationBootstrap testApplicationBootstrap;

	private SingleNodeApplication singleNodeApplication;


	@Before
	public final void setUp() {
		this.testApplicationBootstrap = new TestApplicationBootstrap();
		this.singleNodeApplication = testApplicationBootstrap.getSingleNodeApplication();
		String[] args = {};
		args = addArgIfProvided(args, "transport", getTransport());
		singleNodeApplication.run(args);

		this.pluginContext = (AbstractApplicationContext) this.singleNodeApplication.pluginContext();
		this.containerContext = (AbstractApplicationContext) this.singleNodeApplication.containerContext();
		this.adminContext = (AbstractApplicationContext) this.singleNodeApplication.adminContext();
		setupApplicationContext(this.containerContext);
	}

	protected void setupApplicationContext(ApplicationContext context) {
	}

	protected abstract String getTransport();

	protected abstract Class<? extends MessageBus> getExpectedMessageBusType();

	@After
	public final void shutDown() {
		this.singleNodeApplication.close();
	}

	@Test
	public final void environmentMatchesTransport() {
		MessageBus messageBus = this.containerContext.getBean(MessageBus.class);
		assertEquals(getExpectedMessageBusType(), messageBus.getClass());
	}

	private String[] addArgIfProvided(String[] args, String argName, String argVal) {
		if (StringUtils.hasText(argVal)) {
			args = Arrays.copyOf(args, args.length + 2);
			args[args.length - 2] = "--" + argName;
			args[args.length - 1] = argVal;
		}
		return args;
	}

}
