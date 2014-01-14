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

package org.springframework.xd.dirt.plugins.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.springframework.integration.test.matcher.PayloadMatcher.hasPayload;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.x.bus.AbstractTestMessageBus;
import org.springframework.integration.x.bus.LocalMessageBus;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.validation.BindException;
import org.springframework.xd.dirt.server.AdminServerApplication;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.SimpleModule;
import org.springframework.xd.test.RandomConfigurationSupport;
import org.springframework.xd.module.options.ModuleOptionsMetadata;

/**
 * 
 * @author Michael Minella
 * @author Gunnar Hillert
 * @author Gary Russell
 * 
 */
public class JobPluginTests extends RandomConfigurationSupport {

	private JobPlugin plugin;

	private ConfigurableApplicationContext sharedContext;

	protected AbstractTestMessageBus testMessageBus;

	@After
	public void tearDown() {
		if (sharedContext != null) {
			sharedContext.close();
		}
	}

	@Configuration
	@ImportResource("classpath:/META-INF/spring-xd/batch/batch.xml")
	@EnableAutoConfiguration
	@EnableBatchProcessing
	public static class SharedConfiguration {

	}

	@Before
	public void setUp() throws Exception {
		plugin = new JobPlugin();
		sharedContext = new SpringApplicationBuilder(SharedConfiguration.class).profiles(
				AdminServerApplication.ADMIN_PROFILE, AdminServerApplication.HSQL_PROFILE).properties(
				"spring.datasource.url=jdbc:hsqldb:mem:xdjobrepotest") //
		.web(false).initializers(
				new ApplicationContextInitializer<ConfigurableApplicationContext>() {

					@Override
					public void initialize(ConfigurableApplicationContext applicationContext) {
						plugin.preProcessSharedContext(applicationContext);
					}

				}).run();
	}

	@Test
	public void streamNameAdded() {
		Module module = new SimpleModule(new ModuleDefinition("testJob", ModuleType.job),
				new DeploymentMetadata(
						"foo", 0));

		assertEquals(0, module.getProperties().size());
		plugin.preProcessModule(module);

		Properties moduleProperties = module.getProperties();

		assertEquals("foo", moduleProperties.getProperty("xd.stream.name"));
	}

	@Test
	public void jobOptionsDefaults() throws BindException {
		ModuleOptionsMetadata metadata = new JobPluginMetadataResolver().resolve(new ModuleDefinition("foo",
				ModuleType.job));

		Map<String, String> emptyMap = Collections.emptyMap();
		EnumerablePropertySource<?> ps = metadata.interpolate(emptyMap).asPropertySource();
		assertEquals(true, ps.getProperty("makeUnique"));
		assertEquals("", ps.getProperty("dateFormat"));
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
		when(module.getDeploymentMetadata()).thenReturn(new DeploymentMetadata(moduleGroupName, moduleIndex));
		MessageBus bus = getMessageBus();
		when(module.getComponent(MessageBus.class)).thenReturn(bus);
		MessageChannel stepsOut = new DirectChannel();
		when(module.getComponent("stepExecutionRequests.output", MessageChannel.class)).thenReturn(stepsOut);
		PollableChannel stepResultsIn = new QueueChannel();
		when(module.getComponent("stepExecutionReplies.input", MessageChannel.class)).thenReturn(stepResultsIn);
		PollableChannel stepsIn = new QueueChannel();
		when(module.getComponent("stepExecutionRequests.input", MessageChannel.class)).thenReturn(stepsIn);
		MessageChannel stepResultsOut = new DirectChannel();
		when(module.getComponent("stepExecutionReplies.output", MessageChannel.class)).thenReturn(stepResultsOut);
		plugin.preProcessModule(module);
		plugin.postProcessModule(module);
		checkBusBound(bus);
		stepsOut.send(new GenericMessage<String>("foo"));
		Message<?> stepExecutionRequest = stepsIn.receive(10000);
		assertThat(stepExecutionRequest, hasPayload("foo"));
		stepResultsOut.send(MessageBuilder.withPayload("bar")
				.copyHeaders(stepExecutionRequest.getHeaders()) // replyTo
				.build());
		assertThat(stepResultsIn.receive(10000), hasPayload("bar"));
		plugin.removeModule(module);
		checkBusUnbound(bus);
	}


	protected MessageBus getMessageBus() {
		return new LocalMessageBus();
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
		Mockito.when(module.getDeploymentMetadata()).thenReturn(new DeploymentMetadata("job", 0));

		GenericApplicationContext context = new GenericApplicationContext();
		plugin.preProcessModule(module);
		plugin.preProcessSharedContext(context);

		Mockito.verify(module).addComponents(Matchers.any(Resource.class));

		// TODO: assert that the right resource was added.
		// assertTrue(names.contains("registrar"));
		// assertTrue(names.contains("jobFactoryBean"));
		// assertTrue(names.contains("jobLaunchRequestTransformer"));
		// assertTrue(names.contains("jobLaunchingMessageHandler"));
		// assertTrue(names.contains("input"));
		// assertTrue(names.contains("jobLaunchingChannel"));
		// assertTrue(names.contains("notifications"));
	}

	/**
	 * There should not be any shared beans for the plugin. As per XD-703 the common job beans are registered in the
	 * global common context, so that they are shared across xd-admin/xd-container.
	 */
	@Test
	public void sharedComponentsAdded() {
		GenericApplicationContext context = new GenericApplicationContext();
		plugin.preProcessSharedContext(context);
		List<BeanFactoryPostProcessor> sharedBeans = context.getBeanFactoryPostProcessors();
		assertEquals(0, sharedBeans.size());
	}

	@Test
	public void testThatNotificationChannelIsBound() {

		final Module module = new SimpleModule(new ModuleDefinition("myjob", ModuleType.job),
				new DeploymentMetadata(
						"myjob", 0));

		final TestMessageBus messageBus = new TestMessageBus();
		final DirectChannel inputChannel = new DirectChannel();
		final DirectChannel notificationChannel = new DirectChannel();

		final Module spiedModule = spy(module);

		doReturn(messageBus).when(spiedModule).getComponent(MessageBus.class);
		doReturn(inputChannel).when(spiedModule).getComponent("input", MessageChannel.class);
		doReturn(notificationChannel).when(spiedModule).getComponent("notifications", MessageChannel.class);
		doReturn(null).when(spiedModule).getComponent("stepExecutionRequests.output", MessageChannel.class);

		plugin.postProcessModule(spiedModule);

		assertEquals(Integer.valueOf(1), Integer.valueOf(messageBus.getConsumerNames().size()));
		assertEquals(Integer.valueOf(1), Integer.valueOf(messageBus.getProducerNames().size()));

		assertEquals("job:myjob", messageBus.getConsumerNames().get(0));
		assertEquals("job:myjob-notifications", messageBus.getProducerNames().get(0));

	}

	private class TestMessageBus implements MessageBus {

		private List<String> consumerNames = new ArrayList<String>();

		private List<String> producerNames = new ArrayList<String>();

		@Override
		public void bindConsumer(String name, MessageChannel moduleInputChannel,
				Collection<MediaType> acceptedMediaTypes, boolean aliasHint) {
			consumerNames.add(name);
		}

		@Override
		public void bindPubSubConsumer(String name, MessageChannel inputChannel,
				Collection<MediaType> acceptedMediaTypes) {
			Assert.fail("Should not be called.");
		}

		@Override
		public void bindProducer(String name, MessageChannel moduleOutputChannel, boolean aliasHint) {
			producerNames.add(name);
		}

		@Override
		public void bindPubSubProducer(String name, MessageChannel outputChannel) {
			Assert.fail("Should not be called.");
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
		public void bindRequestor(String name, MessageChannel requests, MessageChannel replies) {
			Assert.fail("Should not be called.");
		}

		@Override
		public void bindReplier(String name, MessageChannel requests, MessageChannel replies) {
			Assert.fail("Should not be called.");
		}

	}

	@After
	public void cleanupMessageBus() {
		if (testMessageBus != null) {
			testMessageBus.cleanup();
		}
	}

}
