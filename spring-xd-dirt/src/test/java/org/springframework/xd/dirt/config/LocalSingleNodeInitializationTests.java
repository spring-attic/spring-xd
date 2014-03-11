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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import org.springframework.integration.x.bus.LocalMessageBus;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.messaging.MessageChannel;
import org.springframework.xd.dirt.server.ContainerRegistrar;
import org.springframework.xd.module.core.Plugin;

/**
 * @author David Turanski
 */
public class LocalSingleNodeInitializationTests extends AbstractSingleNodeInitializationTests {

	@Test
	public final void verifyContextConfiguration() {

		assertSame(pluginContext, containerContext.getParent());
		assertTrue(containerContext.containsBean("moduleDeployer") && !pluginContext.containsBean("moduleDeployer"));
		assertTrue(containerContext.containsBean("containerControlChannel")
				&& !pluginContext.containsBean("containerControlChannel"));
		containerContext.getBean(ContainerRegistrar.class);
		assertEquals(0, pluginContext.getBeansOfType(ContainerRegistrar.class).size());
		Map<String, Plugin> pluginMap = pluginContext.getBeansOfType(Plugin.class);
		assertTrue(pluginMap.size() > 0);
	}

	@Override
	protected void cleanup() {
	}

	@Override
	protected String getTransport() {
		return "local";
	}

	@Override
	protected Class<? extends MessageBus> getExpectedMessageBusType() {
		return LocalMessageBus.class;
	}

	@Override
	protected MessageChannel getControlChannel() {
		return containerContext.getBean("containerControlChannel", MessageChannel.class);
	}

}
