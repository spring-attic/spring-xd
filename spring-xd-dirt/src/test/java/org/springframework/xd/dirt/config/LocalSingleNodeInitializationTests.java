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

import org.springframework.xd.dirt.cluster.ContainerAttributes;
import org.springframework.xd.dirt.cluster.AdminAttributes;
import org.springframework.xd.dirt.integration.bus.local.LocalMessageBus;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.server.ApplicationUtils;
import org.springframework.xd.dirt.server.container.ContainerRegistrar;
import org.springframework.xd.dirt.util.RuntimeUtils;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.module.core.Plugin;

/**
 * @author David Turanski
 * @author Janne Valkealahti
 */
public class LocalSingleNodeInitializationTests extends AbstractSingleNodeInitializationTests {

	@Test
	public final void verifyContextConfiguration() {
		assertSame(pluginContext, containerContext.getParent());
		assertTrue(containerContext.containsBean("moduleDeployer") && !pluginContext.containsBean("moduleDeployer"));
		assertTrue(adminContext.containsBean("messageBus"));
		containerContext.getBean(ContainerRegistrar.class);
		assertEquals(0, pluginContext.getBeansOfType(ContainerRegistrar.class).size());
		Map<String, Plugin> pluginMap = pluginContext.getBeansOfType(Plugin.class);
		assertTrue(pluginMap.size() > 0);

		assertSame(containerContext.getBean(ZooKeeperConnection.class), adminContext.getBean(ZooKeeperConnection.class));

		ContainerAttributes containerAttributes = containerContext.getBean(ContainerAttributes.class);
		assertEquals(RuntimeUtils.getHost(), containerAttributes.getHost());
		ApplicationUtils.dumpContainerApplicationContextConfiguration(containerContext);

		AdminAttributes adminAttributes = adminContext.getBean(AdminAttributes.class);
		assertEquals(RuntimeUtils.getHost(), adminAttributes.getHost());
	}

	@Override
	protected String getTransport() {
		return "local";
	}

	@Override
	protected Class<? extends MessageBus> getExpectedMessageBusType() {
		return LocalMessageBus.class;
	}

}
