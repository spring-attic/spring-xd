/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.plugins.stream;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.channel.AbstractPollableChannel;
import org.springframework.integration.channel.AbstractSubscribableChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.integration.x.bus.serializer.AbstractCodec;
import org.springframework.integration.x.bus.serializer.CompositeCodec;
import org.springframework.integration.x.bus.serializer.MultiTypeCodec;
import org.springframework.integration.x.bus.serializer.kryo.PojoCodec;
import org.springframework.integration.x.bus.serializer.kryo.TupleCodec;
import org.springframework.integration.x.rabbit.RabbitMessageBus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.Module;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.SimpleModule;
import org.springframework.xd.test.rabbit.RabbitTestSupport;
import org.springframework.xd.tuple.Tuple;


/**
 * 
 * @author Gary Russell
 */
public class RabbitMasterSlavePluginTests {

	@Rule
	public RabbitTestSupport rabbitAvailableRule = new RabbitTestSupport();

	@Test
	public void testSimple() {
		RabbitMessageBus bus = new RabbitMessageBus(rabbitAvailableRule.getResource(), getCodec());
		MasterSlavePlugin plugin = new MasterSlavePlugin();

		Module module = mock(Module.class);
		when(module.getDeploymentMetadata()).thenReturn(new DeploymentMetadata("foo", 1));
		when(module.getType()).thenReturn(ModuleType.processor);
		when(module.getName()).thenReturn("testing");
		when(module.getComponent(MessageBus.class)).thenReturn(bus);
		AbstractSubscribableChannel requestsOut = new DirectChannel();
		AbstractPollableChannel requestsIn = new QueueChannel();
		AbstractSubscribableChannel responsesOut = new DirectChannel();
		AbstractPollableChannel responsesIn = new QueueChannel();
		when(module.getComponent("requests.out", MessageChannel.class)).thenReturn(requestsOut);
		when(module.getComponent("requests.in", MessageChannel.class)).thenReturn(requestsIn);
		when(module.getComponent("responses.out", MessageChannel.class)).thenReturn(responsesOut);
		when(module.getComponent("responses.in", MessageChannel.class)).thenReturn(responsesIn);

		plugin.postProcessModule(module);

		requestsOut.send(new GenericMessage<>("foo"));
		Message<?> request = requestsIn.receive(5000);
		assertNotNull(request);

		responsesOut.send(MessageBuilder.withPayload("bar").copyHeaders(request.getHeaders()).build());
		assertNotNull(responsesIn.receive(5000));
	}

	@Test
	public void testContextModule() {
		ModuleDefinition definition = new ModuleDefinition("foo", ModuleType.processor, new ClassPathResource(
				"RabbitMasterSlavePluginTests-context.xml", this.getClass()));
		DeploymentMetadata metadata = new DeploymentMetadata("bar", 1);
		Module module = new SimpleModule(definition, metadata);
		RabbitMessageBus bus = new RabbitMessageBus(rabbitAvailableRule.getResource(), getCodec());
		GenericApplicationContext context = new GenericApplicationContext();
		context.getBeanFactory().registerSingleton("bus", bus);
		context.refresh();
		module.setParentContext(context);
		MasterSlavePlugin plugin = new MasterSlavePlugin();
		module.initialize();
		plugin.postProcessModule(module);

		ApplicationContext moduleContext = TestUtils.getPropertyValue(module, "context", ApplicationContext.class);
		MessageChannel requestsOut = moduleContext.getBean("requests.out", MessageChannel.class);
		requestsOut.send(new GenericMessage<>("foo"));

		PollableChannel responsesIn = moduleContext.getBean("responses.in", PollableChannel.class);
		@SuppressWarnings("unchecked")
		Message<String> response = (Message<String>) responsesIn.receive(5000);
		assertNotNull(response);
		assertThat(response.getPayload(), equalTo("FOO"));
	}

	@SuppressWarnings("unchecked")
	protected MultiTypeCodec<Object> getCodec() {
		Map<Class<?>, AbstractCodec<?>> codecs = new HashMap<Class<?>, AbstractCodec<?>>();
		codecs.put(Tuple.class, new TupleCodec());
		return new CompositeCodec(codecs, new PojoCodec());
	}

}
