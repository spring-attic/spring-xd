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
import java.util.List;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.x.bus.AbstractTestMessageBus;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.integration.x.bus.serializer.AbstractCodec;
import org.springframework.integration.x.bus.serializer.CompositeCodec;
import org.springframework.integration.x.bus.serializer.MultiTypeCodec;
import org.springframework.integration.x.bus.serializer.kryo.PojoCodec;
import org.springframework.integration.x.bus.serializer.kryo.TupleCodec;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.SocketUtils;
import org.springframework.xd.dirt.config.TestMessageBusInjection;
import org.springframework.xd.dirt.container.ContainerAttributes;
import org.springframework.xd.dirt.core.ModuleDeploymentsPath;
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
 * Base class for testing stream deployments across different transport types.
 * Subclasses should define their transport type by implementing a static method
 * with a @BeforeClass annotation, for example:
 *
 * <pre>
 * &#064;BeforeClass
 * public static void setUp() {
 *     setUp("redis");
 * }
 * </pre>
 *
 * Additionally, extensions of this class should initialize the {@link #testMessageBus}
 * member via an {@link org.junit.rules.ExternalResource} static member annotated with
 * {@link org.junit.ClassRule} if the tests require a specific
 * {@link org.springframework.integration.x.bus.MessageBus} implementation.
 *
 * @author David Turanski
 * @author Gunnar Hillert
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 * @author Gary Russell
 * @author Patrick Peralta
 */
public abstract class AbstractSingleNodeStreamDeploymentIntegrationTests {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(AbstractSingleNodeStreamDeploymentIntegrationTests.class);

	/*
	 * (non JavaDoc) Queue name constants.
	 */

	private static final String QUEUE_ROUTE = "queue:routeit";

	private static final String QUEUE_FOO = "queue:foo";

	private static final String QUEUE_BAR = "queue:bar";

	private static final String TOPIC_FOO = "topic:foo";

	/**
	 * ExternalResource implementation that ensures the single node application
	 * used for testing is shut down after the tests complete.
	 */
	@ClassRule
	public static ExternalResource shutdownApplication = new ExternalResource() {

		@Override
		protected void after() {
			if (singleNodeApplication != null) {
				singleNodeApplication.close();
			}
		}
	};

	/**
	 * Provides random configuration information for testing a single node application.
	 */
	protected static TestApplicationBootstrap testApplicationBootstrap;

	/**
	 * Single node application used to execute tests.
	 */
	protected static SingleNodeApplication singleNodeApplication;

	/**
	 * Support for executing commands against the application, such as stream
	 * creation and module loading.
	 */
	protected static SingleNodeIntegrationTestSupport integrationSupport;

	/**
	 * Message bus used for testing. This member should be initialized by
	 * extensions of this class via an {@link org.junit.rules.ExternalResource}
	 * static member annotated with {@link org.junit.ClassRule} if the tests
	 * require a specific {@code MessageBus} implementation.
	 */
	protected static AbstractTestMessageBus testMessageBus;

	/**
	 * Listener for deployment and undeployment requests via a ZooKeeper path.
	 * This listener tracks writes and deletes to ZooKeeper in order to
	 * verify that modules are being deployed and undeployed in the correct order.
	 */
	protected static final DeploymentsListener deploymentsListener = new DeploymentsListener();


	/**
	 * Set up the test using the given transport.
	 *
	 * @param transport the transport to be used by the test.
	 */
	protected static void setUp(String transport) {
		testApplicationBootstrap = new TestApplicationBootstrap();
		singleNodeApplication = testApplicationBootstrap.getSingleNodeApplication().run("--transport", transport);
		integrationSupport = new SingleNodeIntegrationTestSupport(singleNodeApplication);
		if (transport.equalsIgnoreCase("local")) {
			testMessageBus = null;
		}
		else {
			TestMessageBusInjection.injectMessageBus(singleNodeApplication, testMessageBus);
		}
		ContainerAttributes attributes = singleNodeApplication.containerContext().getBean(ContainerAttributes.class);
		integrationSupport.addPathListener(Paths.build(Paths.MODULE_DEPLOYMENTS, attributes.getId()), deploymentsListener);
	}

	/**
	 * Shut down the message bus after all tests have completed.
	 */
	@AfterClass
	public static void cleanupMessageBus() {
		if (testMessageBus != null) {
			testMessageBus.cleanup();
			testMessageBus = null;
		}
		if (singleNodeApplication.containerContext().isActive()) {
			ContainerAttributes attribs = singleNodeApplication.containerContext().getBean(ContainerAttributes.class);
			integrationSupport.removePathListener(Paths.build(Paths.MODULE_DEPLOYMENTS, attribs.getId()),
					deploymentsListener);
		}
	}

	/**
	 * Undeploy all streams after each test execution.
	 */
	@After
	public void cleanUp() {
		integrationSupport.streamDeployer().undeployAll();
		integrationSupport.streamRepository().deleteAll();
		integrationSupport.streamDefinitionRepository().deleteAll();
	}

	/**
	 * Return the codec used by the message bus.
	 *
	 * @return code used by message bus
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected static MultiTypeCodec<Object> getCodec() {
		Map<Class<?>, AbstractCodec<?>> codecs = new HashMap<Class<?>, AbstractCodec<?>>();
		codecs.put(Tuple.class, new TupleCodec());
		return new CompositeCodec(codecs, new PojoCodec());
	}

	/**
	 * Return the number of bindings to the message bus.
	 *
	 * @return number of bindings to the message bus
	 */
	private int getMessageBusBindingCount() {
		MessageBus bus = testMessageBus != null ? testMessageBus.getMessageBus() : integrationSupport.messageBus();
		DirectFieldAccessor accessor = new DirectFieldAccessor(bus);
		return ((List<?>) accessor.getPropertyValue("bindings")).size();
	}

	@Test
	public final void testRoutingWithSpel() throws InterruptedException {
		final StreamDefinition routerDefinition = new StreamDefinition("routerDefinition",
				QUEUE_ROUTE + " > router --expression=payload.contains('a')?'" + QUEUE_FOO + "':'" + QUEUE_BAR + "'");
		doTest(routerDefinition);
	}

	@Test
	public final void testRoutingWithGroovy() throws InterruptedException {
		StreamDefinition routerDefinition = new StreamDefinition("routerDefinition",
				QUEUE_ROUTE + " > router --script='org/springframework/xd/dirt/stream/router.groovy'");
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
		NamedChannelSource source = new SingleNodeNamedChannelSourceFactory(bus).createNamedChannelSource(TOPIC_FOO);

		source.sendPayload("hello");

		final Object bar1 = bar1sink.receivePayload(10000);
		final Object bar2 = bar2sink.receivePayload(10000);
		assertEquals("hello", bar1);
		assertEquals("hello", bar2);

		source.unbind();
		bar1sink.unbind();
		bar2sink.unbind();
	}

	@Test
	public final void deployAndUndeploy() throws InterruptedException {

		assertEquals(0, integrationSupport.streamRepository().count());
		final int iterations = 5;
		int i;
		for (i = 0; i < iterations; i++) {
			String streamName = "test" + i;
			StreamDefinition definition = new StreamDefinition(streamName,
					"http --port=" + SocketUtils.findAvailableTcpPort()
							+ "| transform --expression=payload | filter --expression=true | log");
			integrationSupport.streamDeployer().save(definition);
			assertTrue(String.format("stream %s (%s) not deployed", streamName, definition),
					integrationSupport.deployStream(definition));
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
		assertEquals(iterations, i);
	}

	@Test
	public void moduleChannelsRegisteredWithMessageBus() {
		StreamDefinition sd = new StreamDefinition("busTest", "http | log");
		int originalBindings = getMessageBusBindingCount();
		assertTrue("Timeout waiting for stream deployment", integrationSupport.createAndDeployStream(sd));
		int newBindings = getMessageBusBindingCount() - originalBindings;
		assertEquals(3, newBindings);
		integrationSupport.undeployAndDestroyStream(sd);
		assertEquals(originalBindings, getMessageBusBindingCount());

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

		MessageBus bus = integrationSupport.messageBus();

		NamedChannelSource source = new SingleNodeNamedChannelSourceFactory(bus).createNamedChannelSource("queue:source");
		NamedChannelSink streamSink = new SingleNodeNamedChannelSinkFactory(bus).createNamedChannelSink("queue:sink");
		NamedChannelSink tapSink = new SingleNodeNamedChannelSinkFactory(bus).createNamedChannelSink("queue:tap");

		// Wait for things to set up before sending
		integrationSupport.streamDeploymentVerifier().waitForDeploy(tapDefinition.getName());

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

		NamedChannelSink foosink = sinkFactory.createNamedChannelSink(QUEUE_FOO);
		NamedChannelSink barsink = sinkFactory.createNamedChannelSink(QUEUE_BAR);

		NamedChannelSource source = new SingleNodeNamedChannelSourceFactory(bus).createNamedChannelSource(QUEUE_ROUTE);

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

	/**
	 * Listener for ZooKeeper paths for module deployment/undeployment.
	 */
	static class DeploymentsListener implements PathChildrenCacheListener {

		/**
		 * Map of module deployment requests keyed by stream name.
		 */
		private ConcurrentMap<String, LinkedBlockingQueue<PathChildrenCacheEvent>> deployQueues
				= new ConcurrentHashMap<String, LinkedBlockingQueue<PathChildrenCacheEvent>>();

		/**
		 * Map of module undeployment requests keyed by stream name.
		 */
		private ConcurrentMap<String, LinkedBlockingQueue<PathChildrenCacheEvent>> undeployQueues
				= new ConcurrentHashMap<String, LinkedBlockingQueue<PathChildrenCacheEvent>>();

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
			ModuleDeploymentsPath path = new ModuleDeploymentsPath(event.getData().getPath());
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

		/**
		 * Return the earliest {@link org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent}
		 * associated with deployment of the stream indicated by {@code streamName} that has not already
		 * been returned. The first invocation will return the first event, the second invocation will
		 * return the second event, etc. If no events are available, this method blocks for up to
		 * five seconds to wait for an event.
		 *
		 * @param streamName name of stream to obtain deployment event for
		 * @return an event for deployment, or null if none available or if the
		 *         executing thread is interrupted
		 */
		public PathChildrenCacheEvent nextDeployEvent(String streamName) {
			try {
				deployQueues.putIfAbsent(streamName, new LinkedBlockingQueue<PathChildrenCacheEvent>());
				return deployQueues.get(streamName).poll(5, TimeUnit.SECONDS);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			}
		}

		/**
		 * Return the earliest {@link org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent}
		 * associated with undeployment of the stream indicated by {@code streamName} that has not already
		 * been returned. The first invocation will return the first event, the second invocation will
		 * return the second event, etc. If no events are available, this method blocks for up to
		 * five seconds to wait for an event.
		 *
		 * @param streamName name of stream to obtain undeployment event for
		 * @return an event for undeployment, or null if none available or if the
		 *         executing thread is interrupted
		 */
		public PathChildrenCacheEvent nextUndeployEvent(String streamName) {
			try {
				undeployQueues.putIfAbsent(streamName, new LinkedBlockingQueue<PathChildrenCacheEvent>());
				return undeployQueues.get(streamName).poll(5, TimeUnit.SECONDS);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			}
		}

	}

}
