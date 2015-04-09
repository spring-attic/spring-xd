/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.xd.dirt.plugins.job;

import static org.hamcrest.Matchers.anyOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.springframework.integration.test.matcher.PayloadMatcher.hasPayload;
import static org.springframework.xd.module.options.spi.ModulePlaceholders.XD_JOB_NAME_KEY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.io.Resource;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.validation.BindException;
import org.springframework.xd.batch.hsqldb.server.HsqlDatasourceConfiguration;
import org.springframework.xd.batch.hsqldb.server.HsqlServerApplication;
import org.springframework.xd.dirt.integration.bus.AbstractTestMessageBus;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.integration.bus.local.LocalMessageBus;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.TestModuleDefinitions;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.ResourceConfiguredModule;
import org.springframework.xd.module.options.ModuleOptionsMetadata;
import org.springframework.xd.test.RandomConfigurationSupport;

/**
 *
 * @author Michael Minella
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Ilayaperumal Gopinathan
 * @author David Turanski
 *
 */
public class JobPluginTests extends RandomConfigurationSupport {

	private JobPlugin jobPlugin;

	private JobPartitionerPlugin jobPartitionerPlugin;

	private ConfigurableApplicationContext sharedContext;

	protected AbstractTestMessageBus<?> testMessageBus;

	private LocalMessageBus messageBus;

	@After
	public void tearDown() {
		if (sharedContext != null) {
			sharedContext.close();
		}
	}

	@Configuration
	@ImportResource({ "classpath:/META-INF/spring-xd/batch/batch.xml",
		"classpath:/META-INF/spring-xd/bus/local-bus.xml" })
	@EnableAutoConfiguration
	public static class SharedConfiguration {

	}

	@Before
	public void setUp() throws Exception {
		sharedContext = new SpringApplicationBuilder(SharedConfiguration.class, HsqlDatasourceConfiguration.class,
				HsqlServerApplication.class)
				.profiles(HsqlServerApplication.HSQLDBSERVER_PROFILE)
				.web(false).run();
		messageBus = sharedContext.getBean(LocalMessageBus.class);
		jobPlugin = new JobPlugin(messageBus);
		jobPartitionerPlugin = new JobPartitionerPlugin(messageBus);
	}

	@Test
	public void streamNameAdded() {
		ModuleDescriptor descriptor = new ModuleDescriptor.Builder()
				.setModuleDefinition(TestModuleDefinitions.dummy("testJob", ModuleType.job))
				.setGroup("foo")
				.setIndex(0)
				.build();
		Module module = new ResourceConfiguredModule(descriptor,
				new ModuleDeploymentProperties());

		assertEquals(0, module.getProperties().size());
		jobPlugin.preProcessModule(module);

		Properties moduleProperties = module.getProperties();

		assertEquals("foo", moduleProperties.getProperty(XD_JOB_NAME_KEY));
	}

	@Test
	public void jobOptionsDefaults() throws BindException {
		ModuleOptionsMetadata metadata = new JobPluginMetadataResolver().resolve(TestModuleDefinitions.dummy("foo",
				ModuleType.job));

		Map<String, String> emptyMap = Collections.emptyMap();
		EnumerablePropertySource<?> ps = metadata.interpolate(emptyMap).asPropertySource();
		assertEquals(true, ps.getProperty("makeUnique"));
		assertEquals("yyyy-MM-dd", ps.getProperty("dateFormat"));
		assertEquals("", ps.getProperty("numberFormat"));
	}

	@Test
	public void partitionedJob() {
		String moduleGroupName = "partitionedJob";
		int moduleIndex = 0;
		Module module = Mockito.mock(Module.class);
		when(module.getType()).thenReturn(ModuleType.job);
		Properties properties = new Properties();
		when(module.getProperties()).thenReturn(properties);
		ModuleDeploymentProperties deploymentProperties = new ModuleDeploymentProperties();
		deploymentProperties.put("consumer.concurrency", "2");
		when(module.getDeploymentProperties()).thenReturn(deploymentProperties);
		when(module.getDescriptor()).thenReturn(
				new ModuleDescriptor.Builder().setGroup(moduleGroupName).setIndex(moduleIndex).setModuleDefinition(
						TestModuleDefinitions.dummy("testjob", ModuleType.job)).build());

		MessageChannel stepsOut = new DirectChannel();
		when(module.getComponent("stepExecutionRequests.output", MessageChannel.class)).thenReturn(stepsOut);
		PollableChannel stepResultsIn = new QueueChannel();
		when(module.getComponent("stepExecutionReplies.input", MessageChannel.class)).thenReturn(stepResultsIn);
		final PollableChannel stepsIn = new QueueChannel();
		DirectChannel boundStepsIn = new DirectChannel();
		boundStepsIn.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				stepsIn.send(MessageBuilder.fromMessage(message)
						.setHeader("thread", Thread.currentThread().getName())
						.build());
			}
		});
		when(module.getComponent("stepExecutionRequests.input", MessageChannel.class)).thenReturn(boundStepsIn);
		MessageChannel stepResultsOut = new DirectChannel();
		when(module.getComponent("stepExecutionReplies.output", MessageChannel.class)).thenReturn(stepResultsOut);
		jobPartitionerPlugin.preProcessModule(module);
		jobPartitionerPlugin.postProcessModule(module);
		checkBusBound(messageBus);
		assertEquals(1, TestUtils.getPropertyValue(messageBus, "reqRepExecutors", Map.class).size());
		stepsOut.send(new GenericMessage<String>("foo1"));
		stepsOut.send(new GenericMessage<String>("foo2"));
		Message<?> stepExecutionRequest1 = stepsIn.receive(10000);
		Message<?> stepExecutionRequest2 = stepsIn.receive(10000);
		assertThat(stepExecutionRequest1, anyOf(hasPayload("foo1"), hasPayload("foo2")));
		assertThat(stepExecutionRequest2, anyOf(hasPayload("foo1"), hasPayload("foo2")));
		stepResultsOut.send(MessageBuilder.withPayload("bar1")
				.copyHeaders(stepExecutionRequest1.getHeaders()) // replyTo
				.build());
		stepResultsOut.send(MessageBuilder.withPayload("bar2")
				.copyHeaders(stepExecutionRequest1.getHeaders()) // replyTo
				.build());
		assertThat(stepResultsIn.receive(10000), anyOf(hasPayload("bar1"), hasPayload("bar2")));
		assertThat(stepResultsIn.receive(10000), anyOf(hasPayload("bar1"), hasPayload("bar2")));
		Set<String> threads = new HashSet<String>();
		threads.add((String) stepExecutionRequest1.getHeaders().get("thread"));
		threads.add((String) stepExecutionRequest2.getHeaders().get("thread"));
		assertEquals(2, threads.size());
		jobPartitionerPlugin.removeModule(module);
		checkBusUnbound(messageBus);
		assertEquals(0, TestUtils.getPropertyValue(messageBus, "reqRepExecutors", Map.class).size());
	}

	protected MessageBus getMessageBus() {
		return messageBus;
	}

	protected void checkBusBound(MessageBus bus) {
		assertEquals(2, TestUtils.getPropertyValue(bus, "requestReplyChannels", Map.class).size());
	}

	protected void checkBusUnbound(MessageBus bus) {
		assertEquals(0, TestUtils.getPropertyValue(bus, "requestReplyChannels", Map.class).size());
	}

	@Test
	public void streamComponentsAdded() {

		Module module = Mockito.mock(Module.class);
		Mockito.when(module.getType()).thenReturn(ModuleType.job);
		Properties properties = new Properties();
		Mockito.when(module.getProperties()).thenReturn(properties);
		Mockito.when(module.getDescriptor()).thenReturn(
				new ModuleDescriptor.Builder().setGroup("job").setIndex(0).setModuleDefinition(
						TestModuleDefinitions.dummy("testjob", ModuleType.job)).build());

		jobPlugin.preProcessModule(module);
		Mockito.verify(module).addSource(Matchers.any(Resource.class));

		// TODO: assert that the right resource was added.
		// assertTrue(names.contains("registrar"));
		// assertTrue(names.contains("jobFactoryBean"));
		// assertTrue(names.contains("jobLaunchRequestTransformer"));
		// assertTrue(names.contains("jobLaunchingMessageHandler"));
		// assertTrue(names.contains("input"));
		// assertTrue(names.contains("jobLaunchingChannel"));
		// assertTrue(names.contains("notifications"));
	}

	@Test
	public void testThatInputOutputChannelsAreBound() {

		ModuleDescriptor moduleDescriptor = new ModuleDescriptor.Builder()
				.setModuleDefinition(TestModuleDefinitions.dummy("myjob", ModuleType.job))
				.setGroup("myjob")
				.setIndex(0)
				.build();

		final Module module = new ResourceConfiguredModule(moduleDescriptor,
				new ModuleDeploymentProperties());

		final TestMessageBus messageBus = new TestMessageBus();
		final JobPlugin plugin = new JobPlugin(messageBus);
		final DirectChannel inputChannel = new DirectChannel();

		final Module spiedModule = spy(module);

		doReturn(inputChannel).when(spiedModule).getComponent("input", MessageChannel.class);
		doReturn(null).when(spiedModule).getComponent("output", MessageChannel.class);
		doReturn(null).when(spiedModule).getComponent("stepExecutionRequests.output", MessageChannel.class);

		plugin.postProcessModule(spiedModule);

		assertEquals(Integer.valueOf(1), Integer.valueOf(messageBus.getConsumerNames().size()));
		assertEquals(Integer.valueOf(0), Integer.valueOf(messageBus.getProducerNames().size()));

		assertEquals("job:myjob", messageBus.getConsumerNames().get(0));

	}

	@Test
	public void testThatJobEventsChannelsAreBound() {

		ModuleDescriptor moduleDescriptor = new ModuleDescriptor.Builder()
				.setModuleDefinition(TestModuleDefinitions.dummy("myjob", ModuleType.job))
				.setGroup("myjob")
				.setIndex(0)
				.build();

		final Module module = new ResourceConfiguredModule(moduleDescriptor,
				new ModuleDeploymentProperties());

		final TestMessageBus messageBus = new TestMessageBus();
		final JobEventsListenerPlugin eventsListenerPlugin = new JobEventsListenerPlugin(messageBus);
		final SubscribableChannel jobExecutionEventsChannel = new PublishSubscribeChannel();
		final SubscribableChannel stepExecutionEventsChannel = new PublishSubscribeChannel();
		final SubscribableChannel chunkEventsChannel = new PublishSubscribeChannel();
		final SubscribableChannel itemEventsChannel = new PublishSubscribeChannel();
		final SubscribableChannel skipEventsChannel = new PublishSubscribeChannel();
		final SubscribableChannel aggregatedEventsChannel = new PublishSubscribeChannel();

		final Module spiedModule = spy(module);

		doReturn(messageBus).when(spiedModule).getComponent(MessageBus.class);
		doReturn(jobExecutionEventsChannel).when(spiedModule).getComponent("xd.job.jobExecutionEvents",
				SubscribableChannel.class);
		doReturn(stepExecutionEventsChannel).when(spiedModule).getComponent("xd.job.stepExecutionEvents",
				SubscribableChannel.class);
		doReturn(chunkEventsChannel).when(spiedModule).getComponent("xd.job.chunkEvents", SubscribableChannel.class);
		doReturn(itemEventsChannel).when(spiedModule).getComponent("xd.job.itemEvents", SubscribableChannel.class);
		doReturn(skipEventsChannel).when(spiedModule).getComponent("xd.job.skipEvents", SubscribableChannel.class);
		doReturn(aggregatedEventsChannel).when(spiedModule).getComponent("xd.job.aggregatedEvents",
				SubscribableChannel.class);

		eventsListenerPlugin.preProcessModule(spiedModule);
		eventsListenerPlugin.postProcessModule(spiedModule);

		assertEquals(Integer.valueOf(0), Integer.valueOf(messageBus.getConsumerNames().size()));
		assertEquals(Integer.valueOf(6), Integer.valueOf(messageBus.getProducerNames().size()));

		assertTrue(messageBus.getProducerNames().contains("tap:job:myjob.job"));
		assertTrue(messageBus.getProducerNames().contains("tap:job:myjob.step"));
		assertTrue(messageBus.getProducerNames().contains("tap:job:myjob.chunk"));
		assertTrue(messageBus.getProducerNames().contains("tap:job:myjob.item"));
		assertTrue(messageBus.getProducerNames().contains("tap:job:myjob.skip"));
		assertTrue(messageBus.getProducerNames().contains("tap:job:myjob"));

	}

	private class TestMessageBus implements MessageBus {

		private List<String> consumerNames = new ArrayList<String>();

		private List<String> producerNames = new ArrayList<String>();

		@Override
		public void bindConsumer(String name, MessageChannel moduleInputChannel, Properties properties) {
			consumerNames.add(name);
		}

		@Override
		public void bindPubSubConsumer(String name, MessageChannel inputChannel, Properties properties) {
			Assert.fail("Should not be called.");
		}

		@Override
		public void bindProducer(String name, MessageChannel moduleOutputChannel, Properties properties) {
			producerNames.add(name);
		}

		@Override
		public void bindPubSubProducer(String name, MessageChannel outputChannel, Properties properties) {
			producerNames.add(name);
		}

		@Override
		public void unbindConsumers(String name) {
			Assert.fail("Should be not be called.");
		}

		@Override
		public void unbindProducers(String name) {
			Assert.fail("Should not be called.");
		}

		@Override
		public void unbindConsumer(String name, MessageChannel channel) {
			Assert.fail("Should not be called.");
		}

		@Override
		public void unbindProducer(String name, MessageChannel channel) {
			Assert.fail("Should not be called.");
		}

		public List<String> getConsumerNames() {
			return consumerNames;
		}

		public List<String> getProducerNames() {
			return producerNames;
		}

		@Override
		public void bindRequestor(String name, MessageChannel requests, MessageChannel replies,
				Properties properties) {
			Assert.fail("Should not be called.");
		}

		@Override
		public void bindReplier(String name, MessageChannel requests, MessageChannel replies,
				Properties properties) {
			Assert.fail("Should not be called.");
		}

		@Override
		public MessageChannel bindDynamicProducer(String name, Properties properties) {
			Assert.fail("Should not be called.");
			return null;
		}

		@Override
		public MessageChannel bindDynamicPubSubProducer(String name, Properties properties) {
			Assert.fail("Should not be called.");
			return null;
		}

		@Override
		public boolean isCapable(Capability capability) {
			return false;
		}
	}

	@After
	public void cleanupMessageBus() {
		if (testMessageBus != null) {
			testMessageBus.cleanup();
		}
	}

}
