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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import org.springframework.integration.x.bus.AbstractTestMessageBus;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.integration.x.bus.serializer.AbstractCodec;
import org.springframework.integration.x.bus.serializer.CompositeCodec;
import org.springframework.integration.x.bus.serializer.MultiTypeCodec;
import org.springframework.integration.x.bus.serializer.kryo.PojoCodec;
import org.springframework.integration.x.bus.serializer.kryo.TupleCodec;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.xd.dirt.config.TestMessageBusInjection;
import org.springframework.xd.dirt.container.ContainerMetadata;
import org.springframework.xd.dirt.core.DeploymentsPath;
import org.springframework.xd.dirt.integration.test.SingleNodeIntegrationTestSupport;
import org.springframework.xd.dirt.integration.test.sink.NamedChannelSink;
import org.springframework.xd.dirt.integration.test.sink.SingleNodeNamedChannelSinkFactory;
import org.springframework.xd.dirt.integration.test.source.NamedChannelSource;
import org.springframework.xd.dirt.integration.test.source.SingleNodeNamedChannelSourceFactory;
import org.springframework.xd.dirt.server.SingleNodeApplication;
import org.springframework.xd.dirt.server.TestApplicationBootstrap;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.tuple.Tuple;

/**
 * Base class that contains the tests but does not provide the transport. Each subclass should implement
 * {@link AbstractStreamDeploymentIntegrationTests#getTransport()} in order to execute the test methods defined here for
 * that transport.
 * 
 * @author David Turanski
 * @author Gunnar Hillert
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 * @author Gary Russell
 */
public abstract class AbstractSingleNodeStreamDeploymentIntegrationTests {

	// private static final QueueChannel tapChannel = new QueueChannel();

	@ClassRule
	public static ExternalResource shutdownApplication = new ExternalResource() {

		@Override
		protected void after() {
			if (singleNodeApplication != null) {
				singleNodeApplication.close();
			}
		}
	};

	private final String queueRoute = "queue:routeit";

	private final String queueFoo = "queue:foo";

	private final String queueBar = "queue:bar";

	private final String topicFoo = "topic:foo";

	protected static TestApplicationBootstrap testApplicationBootstrap;

	protected static SingleNodeApplication singleNodeApplication;

	protected static SingleNodeIntegrationTestSupport integrationSupport;

	protected static AbstractTestMessageBus testMessageBus;

	protected static final DeploymentsListener deploymentsListener = new DeploymentsListener();

	@Test
	public final void testRoutingWithSpel() throws InterruptedException {
		final StreamDefinition routerDefinition = new StreamDefinition("routerDefinition",
				queueRoute + " > router --expression=payload.contains('a')?'" + queueFoo + "':'" + queueBar + "'");
		doTest(routerDefinition);
	}

	@Test
	public final void testRoutingWithGroovy() throws InterruptedException {
		StreamDefinition routerDefinition = new StreamDefinition("routerDefinition",
				queueRoute + " > router --script='org/springframework/xd/dirt/stream/router.groovy'");
		doTest(routerDefinition);
	}

	@Test
	public void testBasicTap() {

		StreamDefinition streamDefinition = new StreamDefinition(
				"mystream",
				"queue:source >  transform --expression=payload.toUpperCase() > queue:sink"
				);
		StreamDefinition tapDefinition = new StreamDefinition("mytap",
				"tap:stream:mystream > transform --expression=payload.replaceAll('A','.') > queue:tap");
		tapTest(streamDefinition, tapDefinition);
	}

	@Test
	public void testTappingWithLabels() {

		StreamDefinition streamDefinition = new StreamDefinition(
				"streamWithLabels",
				"queue:source > flibble: transform --expression=payload.toUpperCase() > queue:sink"
				);

		StreamDefinition tapDefinition = new StreamDefinition("tapWithLabels",
				"tap:stream:streamWithLabels.flibble > transform --expression=payload.replaceAll('A','.') > queue:tap");
		tapTest(streamDefinition, tapDefinition);
	}

	// XD-1173
	@Test
	public void testTappingWithRepeatedModulesDoesNotDuplicateMessages() {

		StreamDefinition streamDefinition = new StreamDefinition(
				"streamWithMultipleTransformers",
				"queue:source > flibble: transform --expression=payload.toUpperCase() | transform --expression=payload.toUpperCase() > queue:sink"
				);

		StreamDefinition tapDefinition = new StreamDefinition("tapWithLabels",
				"tap:stream:streamWithMultipleTransformers.flibble > transform --expression=payload.replaceAll('A','.') > queue:tap");
		tapTest(streamDefinition, tapDefinition);
	}

	@Test
	public final void testTopicChannel() throws InterruptedException {

		StreamDefinition bar1Definition = new StreamDefinition("bar1Definition",
				"topic:foo > queue:bar1");
		StreamDefinition bar2Definition = new StreamDefinition("bar2Definition",
				"topic:foo > queue:bar2");
		assertEquals(0, integrationSupport.streamRepository().count());
		integrationSupport.streamDeployer().save(bar1Definition);
		integrationSupport.deployStream(bar1Definition);

		integrationSupport.streamDeployer().save(bar2Definition);
		integrationSupport.deployStream(bar2Definition);
		Thread.sleep(1000);
		assertEquals(2, integrationSupport.streamRepository().count());

		MessageBus bus = integrationSupport.messageBus();

		SingleNodeNamedChannelSinkFactory sinkFactory = new SingleNodeNamedChannelSinkFactory(bus);

		NamedChannelSink bar1sink = sinkFactory.createNamedChannelSink("queue:bar1");
		NamedChannelSink bar2sink = sinkFactory.createNamedChannelSink("queue:bar2");
		NamedChannelSource source = new SingleNodeNamedChannelSourceFactory(bus).createNamedChannelSource(topicFoo);

		source.sendPayload("hello");

		final Object bar1 = bar1sink.receivePayload(10000);
		final Object bar2 = bar2sink.receivePayload(10000);
		assertEquals("hello", bar1);
		assertEquals("hello", bar2);

		source.unbind();
		bar1sink.unbind();
		bar2sink.unbind();
	}

	protected static void setUp(String transport) {
		testApplicationBootstrap = new TestApplicationBootstrap();
		singleNodeApplication = testApplicationBootstrap.getSingleNodeApplication().run("--transport", transport);
		integrationSupport = new SingleNodeIntegrationTestSupport(singleNodeApplication);
		if (testMessageBus != null && !transport.equalsIgnoreCase("local")) {
			TestMessageBusInjection.injectMessageBus(singleNodeApplication, testMessageBus);
		}
		ContainerMetadata cm = singleNodeApplication.containerContext().getBean(ContainerMetadata.class);
		integrationSupport.addPathListener(Paths.build(Paths.DEPLOYMENTS, cm.getId()), deploymentsListener);
	}

	@AfterClass
	public static void cleanupMessageBus() {
		if (testMessageBus != null) {
			testMessageBus.cleanup();
			testMessageBus = null;
		}
		if (singleNodeApplication.containerContext().isActive()) {
			ContainerMetadata cm = singleNodeApplication.containerContext().getBean(ContainerMetadata.class);
			integrationSupport.removePathListener(Paths.build(Paths.DEPLOYMENTS, cm.getId()), deploymentsListener);
		}
	}

	@After
	public void cleanUp() {
		integrationSupport.streamDeployer().undeployAll();
		integrationSupport.streamRepository().deleteAll();
		integrationSupport.streamDefinitionRepository().deleteAll();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected static MultiTypeCodec<Object> getCodec() {
		Map<Class<?>, AbstractCodec<?>> codecs = new HashMap<Class<?>, AbstractCodec<?>>();
		codecs.put(Tuple.class, new TupleCodec());
		return new CompositeCodec(codecs, new PojoCodec());
	}

	@Test
	public final void deployAndUndeploy() throws InterruptedException {

		assertEquals(0, integrationSupport.streamRepository().count());
		final int ITERATIONS = 5;
		int i = 0;
		for (i = 0; i < ITERATIONS; i++) {
			String streamName = "test" + i;
			StreamDefinition definition = new StreamDefinition(streamName,
					"http | transform --expression=payload | filter --expression=true | log");
			integrationSupport.streamDeployer().save(definition);
			assertTrue("stream not deployed", integrationSupport.deployStream(definition));
			assertEquals(1, integrationSupport.streamRepository().count());
			assertTrue(integrationSupport.streamRepository().exists("test" + i));
			assertTrue("stream not undeployed", integrationSupport.undeployStream(definition));
			assertEquals(0, integrationSupport.streamRepository().count());
			assertFalse(integrationSupport.streamRepository().exists("test" + i));
			// Deploys in reverse order
			assertModuleRequest(streamName, "log", false);
			assertModuleRequest(streamName, "filter", false);
			assertModuleRequest(streamName, "transform", false);
			assertModuleRequest(streamName, "http", false);
			// Undeploys in stream order
			assertModuleRequest(streamName, "http", true);
			assertModuleRequest(streamName, "transform", true);
			assertModuleRequest(streamName, "filter", true);
			assertModuleRequest(streamName, "log", true);
		}
		assertEquals(ITERATIONS, i);
	}

	protected void assertModuleRequest(String streamName, String moduleName, boolean remove) {
		PathChildrenCacheEvent event = remove ? deploymentsListener.nextUndeployEvent(streamName)
				: deploymentsListener.nextDeployEvent(streamName);
		assertNotNull("deploymentsListener returned a null event", event);
		assertTrue(moduleName + " not found in " + event.getData().getPath() + " (remove = " + remove + ")",
				event.getData().getPath().contains(moduleName));
	}

	private void tapTest(StreamDefinition streamDefinition, StreamDefinition tapDefinition) {
		integrationSupport.createAndDeployStream(streamDefinition);
		integrationSupport.createAndDeployStream(tapDefinition);

		NamedChannelSource source = new SingleNodeNamedChannelSourceFactory(integrationSupport.messageBus()).createNamedChannelSource("queue:source");
		NamedChannelSink streamSink = new SingleNodeNamedChannelSinkFactory(integrationSupport.messageBus()).createNamedChannelSink("queue:sink");
		NamedChannelSink tapSink = new SingleNodeNamedChannelSinkFactory(integrationSupport.messageBus()).createNamedChannelSink("queue:tap");

		// Wait for things to set up before sending
		try {
			Thread.sleep(2000);
		}
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		source.send(new GenericMessage<String>("Dracarys!"));

		Message<?> m1;
		int count1 = 0;
		String result1 = null;
		while ((m1 = streamSink.receive(1000)) != null) {
			count1++;
			result1 = (String) m1.getPayload();
		}

		Message<?> m2;
		int count2 = 0;
		String result2 = null;
		while ((m2 = tapSink.receive(1000)) != null) {
			count2++;
			result2 = (String) m2.getPayload();
		}

		assertEquals("DRACARYS!", result1);
		assertEquals(1, count1);

		assertEquals("DR.C.RYS!", result2);
		assertEquals(1, count2);

		source.unbind();
		streamSink.unbind();
		tapSink.unbind();
		integrationSupport.undeployAndDestroyStream(streamDefinition);
		integrationSupport.undeployAndDestroyStream(tapDefinition);
	}

	private void doTest(StreamDefinition routerDefinition) throws InterruptedException {
		assertEquals(0, integrationSupport.streamRepository().count());
		integrationSupport.streamDeployer().save(routerDefinition);
		assertTrue("stream not deployed", integrationSupport.deployStream(routerDefinition));
		assertEquals(1, integrationSupport.streamRepository().count());

		Thread.sleep(1000);
		assertModuleRequest(routerDefinition.getName(), "router", false);

		MessageBus bus = integrationSupport.messageBus();
		assertNotNull(bus);

		SingleNodeNamedChannelSinkFactory sinkFactory = new SingleNodeNamedChannelSinkFactory(bus);

		NamedChannelSink foosink = sinkFactory.createNamedChannelSink("queue:foo");
		NamedChannelSink barsink = sinkFactory.createNamedChannelSink("queue:bar");

		NamedChannelSource source = new SingleNodeNamedChannelSourceFactory(bus).createNamedChannelSource("queue:routeit");

		source.sendPayload("a");
		source.sendPayload("b");

		final Object fooPayload = foosink.receivePayload(10000);
		final Object barPayload = barsink.receivePayload(10000);

		assertEquals("a", fooPayload);
		assertEquals("b", barPayload);

		source.unbind();
		foosink.unbind();
		barsink.unbind();
	}

	static class DeploymentsListener implements PathChildrenCacheListener  {

		private ConcurrentMap<String, LinkedBlockingQueue<PathChildrenCacheEvent>> deployQueues = new ConcurrentHashMap<String, LinkedBlockingQueue<PathChildrenCacheEvent>>();

		private ConcurrentMap<String, LinkedBlockingQueue<PathChildrenCacheEvent>> undeployQueues = new ConcurrentHashMap<String, LinkedBlockingQueue<PathChildrenCacheEvent>>();

		@Override
		public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
			DeploymentsPath path = new DeploymentsPath(event.getData().getPath());
			if (event.getType().equals(Type.CHILD_ADDED)) {
				deployQueues.putIfAbsent(path.getStreamName(), new LinkedBlockingQueue<PathChildrenCacheEvent>());
				LinkedBlockingQueue<PathChildrenCacheEvent> queue = deployQueues.get(path.getStreamName());
				queue.put(event);
			}
			else if (event.getType().equals(Type.CHILD_REMOVED)) {
				undeployQueues.putIfAbsent(path.getStreamName(), new LinkedBlockingQueue<PathChildrenCacheEvent>());
				LinkedBlockingQueue<PathChildrenCacheEvent> queue = undeployQueues.get(path.getStreamName());
				queue.put(event);
			}
		}

		public PathChildrenCacheEvent nextDeployEvent(String streamName) {
			try {
				LinkedBlockingQueue<PathChildrenCacheEvent> queue = deployQueues.get(streamName);
				return queue != null ? queue.poll(10, TimeUnit.SECONDS) : null;
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			}
		}

		public PathChildrenCacheEvent nextUndeployEvent(String streamName) {
			try {
				LinkedBlockingQueue<PathChildrenCacheEvent> queue = undeployQueues.get(streamName);
				return queue != null ? queue.poll(10, TimeUnit.SECONDS) : null;
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			}
		}

	}
}
