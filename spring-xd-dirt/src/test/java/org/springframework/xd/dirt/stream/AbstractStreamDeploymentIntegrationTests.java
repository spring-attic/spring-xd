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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.messaging.Message;
import org.springframework.xd.dirt.event.AbstractModuleEvent;
import org.springframework.xd.dirt.server.SingleNodeApplication;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.core.Module;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author David Turanski
 */
public abstract class AbstractStreamDeploymentIntegrationTests {

	private AbstractApplicationContext context;

	private SingleNodeApplication application;

	protected StreamDefinitionRepository streamDefinitionRepository;

	protected StreamRepository streamRepository;

	protected StreamDeployer streamDeployer;

	private ModuleEventListener moduleEventListener = new ModuleEventListener();

	private final QueueChannel tapChannel = new QueueChannel();


	@Before
	public final void setUp() {
		String transport = this.getTransport();
		this.application = new SingleNodeApplication();
		application.run("--transport", transport);

		this.context = (AbstractApplicationContext) application.getContainerContext();
		this.streamDefinitionRepository = context.getBean(StreamDefinitionRepository.class);
		this.streamRepository = context.getBean(StreamRepository.class);
		this.streamDeployer = application.getAdminContext().getBean(StreamDeployer.class);

		AbstractMessageChannel deployChannel = application.getAdminContext().getBean("deployChannel",
				AbstractMessageChannel.class);
		AbstractMessageChannel undeployChannel = application.getAdminContext().getBean("undeployChannel",
				AbstractMessageChannel.class);
		deployChannel.addInterceptor(new WireTap(tapChannel));
		undeployChannel.addInterceptor(new WireTap(tapChannel));
		context.addApplicationListener(this.moduleEventListener);
		setupApplicationContext(context);
	}

	@After
	public final void shutDown() {
		cleanup(this.context);
		this.application.close();
	}

	@Test
	public final void deployAndUndeploy() throws InterruptedException {

		assertEquals(0, streamRepository.count());
		final int ITERATIONS = 5;
		int i = 0;
		for (i = 0; i < ITERATIONS; i++) {
			StreamDefinition definition = new StreamDefinition("test" + i,
					"http | transform --expression=payload | filter --expression=true | log");
			streamDeployer.save(definition);
			waitForDeploy(definition);
			assertEquals(1, streamRepository.count());
			assertTrue(streamRepository.exists("test" + i));
			waitForUndeploy(definition);
			assertEquals(0, streamRepository.count());
			assertFalse(streamRepository.exists("test" + i));
			// Deploys in reverse order
			assertModuleRequest("log", false);
			assertModuleRequest("filter", false);
			assertModuleRequest("transform", false);
			assertModuleRequest("http", false);
			// Undeploys in stream order
			assertModuleRequest("http", true);
			assertModuleRequest("transform", true);
			assertModuleRequest("filter", true);
			assertModuleRequest("log", true);
			assertNull(tapChannel.receive(0));
		}
		assertEquals(ITERATIONS, i);

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

	protected Module getModule(String moduleName, int index) {

		final Map<String, Map<Integer, Module>> deployedModules = this.moduleEventListener.getDeployedModules();

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

	protected void deploy(StreamDefinition definition) {
		waitForDeploy(definition);
	}

	private boolean waitForStreamOp(StreamDefinition definition, boolean isDeploy) {
		final int MAX_TRIES = 40;
		int tries = 1;
		boolean done = false;
		while (!done && tries <= MAX_TRIES) {
			done = true;
			int i = definition.getModuleDefinitions().size();
			for (ModuleDefinition module : definition.getModuleDefinitions()) {
				Module deployedModule = getModule(module.getName(), --i);

				done = (isDeploy) ? deployedModule != null : deployedModule == null;
				if (!done) {
					break;
				}
			}
			if (!done) {
				try {
					Thread.sleep(100);
					tries++;
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
		return done;
	}

	private void waitForUndeploy(StreamDefinition definition) {
		streamDeployer.undeploy(definition.getName());
		boolean undeployed = waitForStreamOp(definition, false);
		assertTrue("stream " + definition.getName() + " not undeployed ", undeployed);
	}

	private void waitForDeploy(StreamDefinition definition) {

		streamDeployer.deploy(definition.getName());
		boolean deployed = waitForStreamOp(definition, true);
		assertTrue("stream " + definition.getName() + " not deployed ", deployed);
	}

	static class ModuleEventListener implements ApplicationListener<AbstractModuleEvent> {

		private final ConcurrentMap<String, Map<Integer, Module>> deployedModules = new ConcurrentHashMap<String, Map<Integer, Module>>();

		@Override
		public void onApplicationEvent(AbstractModuleEvent event) {
			Module module = event.getSource();
			if (event.getType().equals("ModuleDeployed")) {
				this.deployedModules.putIfAbsent(module.getDeploymentMetadata().getGroup(),
						new HashMap<Integer, Module>());
				this.deployedModules.get(module.getDeploymentMetadata().getGroup()).put(
						module.getDeploymentMetadata().getIndex(), module);
			}
			else {
				this.deployedModules.get(module.getDeploymentMetadata().getGroup()).remove(
						module.getDeploymentMetadata().getIndex());
			}
		}

		public Map<String, Map<Integer, Module>> getDeployedModules() {
			return this.deployedModules;
		}

	}
}
