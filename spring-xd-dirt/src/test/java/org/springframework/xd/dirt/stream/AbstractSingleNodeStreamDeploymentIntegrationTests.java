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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import org.springframework.context.ApplicationListener;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.xd.dirt.event.AbstractModuleEvent;
import org.springframework.xd.dirt.server.SingleNodeApplication;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.test.RandomConfigurationSupport;


/**
 * Base class that contains the tests but does not provide the transport. Each subclass should implement
 * {@link AbstractStreamDeploymentIntegrationTests#getTransport()} in order to execute the test methods defined here for
 * that transport.
 * 
 * @author David Turanski
 * @author Gunnar Hillert
 * @author Mark Fisher
 */
public abstract class AbstractSingleNodeStreamDeploymentIntegrationTests extends RandomConfigurationSupport {

	protected static AbstractApplicationContext context;

	private static SingleNodeApplication application;

	protected static StreamDefinitionRepository streamDefinitionRepository;

	protected static StreamRepository streamRepository;

	protected static StreamDeployer streamDeployer;

	private static ModuleEventListener moduleEventListener = new ModuleEventListener();

	private static final QueueChannel tapChannel = new QueueChannel();


	@ClassRule
	public static ExternalResource shutdownApplication = new ExternalResource() {

		@Override
		protected void after() {
			if (application != null) {
				application.close();
			}
		}
	};

	protected static final String XD_DEPLOYER_PLACEHOLDER = "${xd.deployer.queue}";

	protected static final String XD_UNDEPLOYER_PLACEHOLDER = "${xd.undeployer.topic}";

	@Test
	public final void testRoutingWithSpel() throws InterruptedException {
		final StreamDefinition routerDefinition = new StreamDefinition("routerDefinition",
				"queue:routeit > router --expression=payload.contains('a')?'queue:foo':'queue:bar'");
		doTest(routerDefinition);
	}

	@Test
	public final void testRoutingWithGroovy() throws InterruptedException {
		StreamDefinition routerDefinition = new StreamDefinition("routerDefinition",
				"queue:routeit > router --script='org/springframework/xd/dirt/stream/router.groovy'");
		doTest(routerDefinition);
	}

	@Test
	public final void testTopicChannel() throws InterruptedException {
		StreamDefinition bar1Definition = new StreamDefinition("bar1Definition",
				"topic:foo > queue:bar1");
		StreamDefinition bar2Definition = new StreamDefinition("bar2Definition",
				"topic:foo > queue:bar2");
		assertEquals(0, streamRepository.count());
		streamDeployer.save(bar1Definition);
		deploy(bar1Definition);

		streamDeployer.save(bar2Definition);
		deploy(bar2Definition);
		Thread.sleep(1000);
		assertEquals(2, streamRepository.count());

		final Module module = getModule("bridge", 0);

		MessageBus bus = module.getComponent(MessageBus.class);

		QueueChannel bar1Channel = new QueueChannel();
		QueueChannel bar2Channel = new QueueChannel();

		bus.bindConsumer("queue:bar1", bar1Channel, Collections.singletonList(MediaType.ALL), true);
		bus.bindConsumer("queue:bar2", bar2Channel, Collections.singletonList(MediaType.ALL), true);

		DirectChannel testChannel = new DirectChannel();
		bus.bindPubSubProducer("topic:foo", testChannel);

		testChannel.send(new GenericMessage<String>("hello"));

		final Message<?> bar1Message = bar1Channel.receive(10000);
		final Message<?> bar2Message = bar2Channel.receive(10000);
		assertEquals("hello", bar1Message.getPayload());
		assertEquals("hello", bar2Message.getPayload());

		bus.unbindProducer("topic:foo", testChannel);
		bus.unbindConsumer("queue:bar1", bar1Channel);
		bus.unbindConsumer("queue:bar2", bar2Channel);
	}


	protected final static void setUp(String transport) {
		application = new SingleNodeApplication();
		application.run("--transport", transport);

		context = (AbstractApplicationContext) application.getContainerContext();
		streamDefinitionRepository = context.getBean(StreamDefinitionRepository.class);
		streamRepository = context.getBean(StreamRepository.class);
		streamDeployer = application.getAdminContext().getBean(StreamDeployer.class);

		AbstractMessageChannel deployChannel = application.getAdminContext().getBean("deployChannel",
				AbstractMessageChannel.class);
		AbstractMessageChannel undeployChannel = application.getAdminContext().getBean("undeployChannel",
				AbstractMessageChannel.class);
		deployChannel.addInterceptor(new WireTap(tapChannel));
		undeployChannel.addInterceptor(new WireTap(tapChannel));
		context.addApplicationListener(moduleEventListener);
	}

	@After
	public void cleanUp() {
		streamRepository.deleteAll();
		streamDefinitionRepository.deleteAll();
		streamDeployer.undeployAll();

		Message<?> msg = tapChannel.receive(1000);
		while (msg != null) {
			msg = tapChannel.receive(1000);
		}

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

		assertTrue(String.format("payload %s does not contain the expected module name %s", payload, moduleName),
				payload.contains("\"module\":\"" + moduleName + "\""));
		assertTrue(String.format("payload %s does not contain the expected remove: value", payload),
				payload.contains("\"remove\":" + (remove ? "true" : "false")));
	}

	protected Module getModule(String moduleName, int index) {

		final Map<String, Map<Integer, Module>> deployedModules = moduleEventListener.getDeployedModules();

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

	private void doTest(StreamDefinition routerDefinition) throws InterruptedException {
		assertEquals(0, streamRepository.count());
		streamDeployer.save(routerDefinition);
		deploy(routerDefinition);
		assertEquals(1, streamRepository.count());
		assertModuleRequest("router", false);

		final Module module = getModule("router", 0);
		MessageBus bus = module.getComponent(MessageBus.class);

		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		bus.bindConsumer("queue:foo", fooChannel, Collections.singletonList(MediaType.ALL), true);
		bus.bindConsumer("queue:bar", barChannel, Collections.singletonList(MediaType.ALL), true);

		DirectChannel testChannel = new DirectChannel();
		bus.bindProducer("queue:routeit", testChannel, true);
		testChannel.send(MessageBuilder.withPayload("a").build());

		testChannel.send(MessageBuilder.withPayload("b").build());

		final Message<?> fooMessage = fooChannel.receive(10000);
		final Message<?> barMessage = barChannel.receive(10000);
		assertEquals("a", fooMessage.getPayload());
		assertEquals("b", barMessage.getPayload());

		bus.unbindProducer("queue:routeit", testChannel);
		bus.unbindConsumer("queue:foo", fooChannel);
		bus.unbindConsumer("queue:bar", barChannel);
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
