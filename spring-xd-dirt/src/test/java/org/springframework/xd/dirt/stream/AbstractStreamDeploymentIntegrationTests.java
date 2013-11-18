/*
 * Copyright 2013 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.xd.dirt.module.ModuleDeployer;
import org.springframework.xd.dirt.server.ParentConfiguration;
import org.springframework.xd.module.Module;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 */
public abstract class AbstractStreamDeploymentIntegrationTests {

	private AbstractApplicationContext context;

	protected StreamDefinitionRepository streamDefinitionRepository;

	protected StreamRepository streamRepository;

	protected StreamDeployer streamDeployer;

	protected ModuleDeployer moduleDeployer;

	private final QueueChannel tapChannel = new QueueChannel();

	@EnableAutoConfiguration
	@Configuration
	@ImportResource({
		"META-INF/spring-xd/internal/container.xml",
		"META-INF/spring-xd/internal/deployers.xml",
		"META-INF/spring-xd/plugins/streams.xml",
		"META-INF/spring-xd/store/memory-store.xml" })
	protected static class StreamDeploymentIntegrationTestsConfiguration {
	}

	@Before
	public final void setUp() {
		String transport = this.getTransport();
		context = (AbstractApplicationContext) new SpringApplicationBuilder(ParentConfiguration.class).profiles(
				"singleNode", "memory").properties("XD_HOME=..").child(
				StreamDeploymentIntegrationTestsConfiguration.class).sources(
				"META-INF/spring-xd/transports/" + transport + "-admin.xml").web(false).run(
				"--XD_TRANSPORT=" + transport);
		this.streamDefinitionRepository = context.getBean(StreamDefinitionRepository.class);
		this.streamRepository = context.getBean(StreamRepository.class);
		this.streamDeployer = context.getBean(StreamDeployer.class);
		this.moduleDeployer = context.getBean(ModuleDeployer.class);

		AbstractMessageChannel deployChannel = context.getBean("deployChannel", AbstractMessageChannel.class);
		AbstractMessageChannel undeployChannel = context.getBean("undeployChannel", AbstractMessageChannel.class);
		deployChannel.addInterceptor(new WireTap(tapChannel));
		undeployChannel.addInterceptor(new WireTap(tapChannel));
		setupApplicationContext(context);
	}

	@After
	public final void shutDown() {
		if (this.context != null) {
			this.cleanup(this.context);
			this.context.close();
		}
	}

	@Test
	public final void deployAndUndeploy() {
		assertEquals(0, streamRepository.count());
		StreamDefinition definition = new StreamDefinition("test1", "time | log");
		streamDefinitionRepository.save(definition);
		streamDeployer.deploy("test1");
		assertEquals(1, streamRepository.count());
		assertTrue(streamRepository.exists("test1"));
		streamDeployer.undeploy("test1");
		assertEquals(0, streamRepository.count());
		assertFalse(streamRepository.exists("test1"));
		assertModuleRequest("log", false);
		assertModuleRequest("time", false);
		assertModuleRequest("time", true);
		assertModuleRequest("log", true);
		assertNull(tapChannel.receive(0));
	}

	protected void assertModuleRequest(String moduleName, boolean remove) {
		Message<?> next = tapChannel.receive(0);
		assertNotNull(next);
		String payload = (String) next.getPayload();
		assertTrue(payload.contains("\"module\":\"" + moduleName + "\""));
		assertTrue(payload.contains("\"remove\":" + (remove ? "true" : "false")));
	}

	protected void setupApplicationContext(ApplicationContext context) {
	}

	protected abstract String getTransport();

	protected abstract void cleanup(ApplicationContext context);

	protected Module getModule(String moduleName, int index, ModuleDeployer moduleDeployer) {

		@SuppressWarnings("unchecked")
		final Map<String, Map<Integer, Module>> deployedModules = TestUtils.getPropertyValue(moduleDeployer,
				"deployedModules", ConcurrentMap.class);

		Module matchedModule = null;
		for (Entry<String, Map<Integer, Module>> entry : deployedModules.entrySet()) {
			final Module module = entry.getValue().get(index);
			if (module != null && moduleName.equals(module.getName())) {
				matchedModule = module;
				break;
			}
		}
		return matchedModule;
	}
}
