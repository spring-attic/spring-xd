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

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.x.channel.registry.ChannelRegistry;
import org.springframework.xd.module.Module;

/**
 * Unit test of {@link ChannelRegistrar}
 *
 * @author Jennifer Hickey
 */
public class ChannelRegistrarTests {

	@Mock
	private ChannelRegistry registry;

	@Mock
	private Module module;

	private ChannelRegistrar channelRegistrar;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		this.channelRegistrar = new ChannelRegistrar(registry, module, "mystream", 1);
	}

	@Test
	public void testPostProcessOutputChannel() throws Exception {
		MessageChannel output = new DirectChannel();
		Object processedOutput = channelRegistrar.postProcessAfterInitialization(output, "output");
		assertSame(output, processedOutput);
		verify(registry).outbound("mystream.1", output, module);
	}

	@Test
	public void testPostProcessInputChannel() throws Exception {
		MessageChannel input = new DirectChannel();
		Object processedInput = channelRegistrar.postProcessAfterInitialization(input, "input");
		assertSame(input, processedInput);
		verify(registry).inbound("mystream.0", input, module);
	}

	@Test
	public void testPostProcessOtherChannel() {
		MessageChannel errorChannel = new DirectChannel();
		assertSame(errorChannel, channelRegistrar.postProcessAfterInitialization(errorChannel, "errorChannel"));
	}

	@Test
	public void testPostProcessNotAChannel() {
		assertSame("foo", channelRegistrar.postProcessAfterInitialization("foo", "myBean"));
	}

	@Test
	public void testDestroy() throws Exception {
		channelRegistrar.destroy();
		verify(registry).cleanAll("mystream.1");
	}

}
