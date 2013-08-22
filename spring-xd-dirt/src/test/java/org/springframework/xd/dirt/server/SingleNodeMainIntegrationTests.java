/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.x.channel.registry.ChannelRegistry;
import org.springframework.xd.analytics.metrics.core.MetricRepository;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.dirt.module.ModuleDeployer;
import org.springframework.xd.dirt.plugins.job.JobPlugin;
import org.springframework.xd.dirt.plugins.stream.StreamPlugin;
import org.springframework.xd.dirt.server.options.SingleNodeOptions;
import org.springframework.xd.dirt.server.options.XDPropertyKeys;
import org.springframework.xd.module.Plugin;


/**
 * @author David Turanski
 * @since 1.0
 * 
 */
public class SingleNodeMainIntegrationTests extends AbstractAdminMainIntegrationTests {

	@Test
	public void testDefault() {
		SingleNodeMain.main(new String[] {});
	}

	@Test
	public void testConfiguration() {
		SingleNodeOptions options = SingleNodeMain.parseOptions(new String[] { "--httpPort", "0", "--transport",
			"local",
			"--store",
			"memory", "--enableJmx", "true", "--analytics", "memory" });
		SingleNodeServer server = SingleNodeMain.launchSingleNodeServer(options);
		AdminServer adminServer = server.getAdminServer();
		XDContainer container = server.getContainer();

		ApplicationContext containerContext = container.getApplicationContext();
		ApplicationContext adminContext = adminServer.getApplicationContext();

		assertNotSame(containerContext, adminContext);

		assertEquals(1, containerContext.getBeansOfType(ModuleDeployer.class).size());
		// No need to assert. Will throw exception
		containerContext.getBean("containerControlChannel", MessageChannel.class);

		assertEquals("container context should not have a channel registry", 0,
				containerContext.getBeansOfType(ChannelRegistry.class).size());

		assertTrue("No plugins loaded into container context",
				containerContext.getBeansOfType(Plugin.class).size() > 0);
		assertTrue("No StreamPlugin loaded into container context",
				containerContext.getBeansOfType(StreamPlugin.class).size() > 0);
		assertEquals("More than one StreamPlugin loaded into container context", 1,
				containerContext.getBeansOfType(StreamPlugin.class).size());

		assertTrue("No JobPlugin loaded into container context",
				containerContext.getBeansOfType(JobPlugin.class).size() > 0);
		assertEquals("More than 1 JobPlugin loaded into container context", 1,
				containerContext.getBeansOfType(JobPlugin.class).size());

		assertEquals("admin context should not have plugins", 0,
				adminContext.getBeansOfType(Plugin.class).size());
		assertEquals("admin context should not have a channel registry", 0,
				adminContext.getBeansOfType(ChannelRegistry.class).size());
		assertEquals("admin context should not have a module deployer", 0,
				adminContext.getBeansOfType(ModuleDeployer.class).size());

		adminContext.getBean("deployChannel", MessageChannel.class);
		adminContext.getBean("undeployChannel", MessageChannel.class);
		try {
			adminContext.getBean("input", MessageChannel.class);
			fail("input channel should not exist in admin context");
		}
		catch (Exception e) {
			// expected
		}
		assertNotNull(adminContext.getParent());
		assertNotNull(containerContext.getParent());
		assertSame(adminContext.getParent(), containerContext.getParent());

		assertTrue("no metrics repositories have been registered in the container context",
				containerContext.getParent().getBeansOfType(MetricRepository.class).size() > 0);
	}

	@Test
	public void testConfigurationOverridesSystemProperties() {

		System.setProperty(XDPropertyKeys.XD_ANALYTICS, "redis");
		System.setProperty(XDPropertyKeys.XD_HTTP_PORT, "8080");
		System.setProperty(XDPropertyKeys.XD_STORE, "redis");

		SingleNodeServer server = SingleNodeMain.launchSingleNodeServer(
				SingleNodeMain.parseOptions(new String[] { "--httpPort", "0", "--transport",
					"local", "--analytics", "memory" }));
		Environment env = server.getAdminServer().getApplicationContext().getEnvironment();

		assertEquals("0", env.getProperty(XDPropertyKeys.XD_HTTP_PORT));
		assertEquals("local", env.getProperty(XDPropertyKeys.XD_TRANSPORT));
		assertEquals("memory", env.getProperty(XDPropertyKeys.XD_ANALYTICS));
		assertEquals("redis", env.getProperty(XDPropertyKeys.XD_STORE));
	}

	@Test
	public void testSystemPropertiesOverridesDefault() {

		System.setProperty(XDPropertyKeys.XD_ANALYTICS, "redis");
		System.setProperty(XDPropertyKeys.XD_HTTP_PORT, "0");
		System.setProperty(XDPropertyKeys.XD_STORE, "redis");

		SingleNodeServer server = SingleNodeMain.launchSingleNodeServer(
				SingleNodeMain.parseOptions(new String[] {}));
		Environment env = server.getAdminServer().getApplicationContext().getEnvironment();

		assertEquals("0", env.getProperty(XDPropertyKeys.XD_HTTP_PORT));
		assertEquals("redis", env.getProperty(XDPropertyKeys.XD_ANALYTICS));
		assertEquals("redis", env.getProperty(XDPropertyKeys.XD_STORE));
	}

	@After
	public void cleanUp() {
		System.clearProperty(XDPropertyKeys.XD_ANALYTICS);
		System.clearProperty(XDPropertyKeys.XD_HTTP_PORT);
		System.clearProperty(XDPropertyKeys.XD_STORE);
	}
}
