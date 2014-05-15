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

package org.springframework.xd.dirt.plugins.stream;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.core.Module;

/**
 * @author Jennifer Hickey
 * @author Gary Russell
 */
public class MessageBusRegistrationTests {

	@Mock
	private MessageBus bus;

	private StreamPlugin streamPlugin;

	@Mock
	private Module module;

	private ModuleDescriptor descriptor = new ModuleDescriptor.Builder()
			.setGroup("mystream")
			.setIndex(1)
			.build();

	private MessageChannel input = new DirectChannel();

	private MessageChannel output = new DirectChannel();

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		streamPlugin = new StreamPlugin(bus);
		when(module.getComponent("input", MessageChannel.class)).thenReturn(input);
		when(module.getComponent("output", MessageChannel.class)).thenReturn(output);
		when(module.getDescriptor()).thenReturn(descriptor);
	}

	@Test
	public void testRegistration() throws Exception {
		streamPlugin.postProcessModule(module);
		verify(bus).bindConsumer("mystream.0", input);
		verify(bus).bindProducer("mystream.1", output);
	}

}
