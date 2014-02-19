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

package org.springframework.xd.dirt.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.xd.module.ModuleType.processor;
import static org.springframework.xd.module.ModuleType.sink;
import static org.springframework.xd.module.ModuleType.source;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.CompositeModule;
import org.springframework.xd.module.core.SimpleModule;

/**
 * @author Mark Fisher
 */
public class CompositeModuleTests {

	private volatile ModuleRegistry moduleRegistry;

	private volatile ModuleDefinition sourceDefinition;

	private volatile ModuleDefinition processor1Definition;

	private volatile ModuleDefinition processor2Definition;

	private volatile ModuleDefinition sinkDefinition;

	@Before
	public void setupModuleDefinitions() {
		moduleRegistry = new ResourceModuleRegistry("file:src/test/resources/testmodules/");
		sourceDefinition = moduleRegistry.findDefinition("source", source);
		processor1Definition = moduleRegistry.findDefinition("testprocessor1", processor);
		processor2Definition = moduleRegistry.findDefinition("testprocessor2", processor);
		sinkDefinition = moduleRegistry.findDefinition("sink", sink);
	}

	@Test
	public void testCompositeSource() {
		DeploymentMetadata metadata = new DeploymentMetadata("compositesourcegroup", 0);
		List<SimpleModule> modules = new ArrayList<SimpleModule>();
		modules.add(new SimpleModule(sourceDefinition, metadata));
		modules.add(new SimpleModule(processor1Definition, metadata));
		modules.add(new SimpleModule(processor2Definition, metadata));
		CompositeModule module = new CompositeModule("compositesource", ModuleType.source, modules, metadata);
		assertEquals(source, module.getType());
	}

	@Test
	public void testCompositeProcessor() {
		DeploymentMetadata metadata = new DeploymentMetadata("compositeprocessorgroup", 1);
		List<SimpleModule> modules = new ArrayList<SimpleModule>();
		modules.add(new SimpleModule(processor1Definition, metadata));
		modules.add(new SimpleModule(processor2Definition, metadata));
		CompositeModule module = new CompositeModule("compositeprocessor", ModuleType.processor, modules, metadata);
		module.initialize();
		module.start();
		assertEquals(processor, module.getType());
		MessageChannel input = module.getComponent("input", MessageChannel.class);
		assertNotNull(input);
		SubscribableChannel output = module.getComponent("output", SubscribableChannel.class);
		assertNotNull(output);
		final AtomicReference<Message<?>> result = new AtomicReference<Message<?>>();
		output.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) {
				result.set(message);
			}
		});
		input.send(MessageBuilder.withPayload("foo").build());
		Message<?> message = result.get();
		assertEquals("foo12", message.getPayload());
	}

	@Test
	public void testCompositeSink() {
		DeploymentMetadata metadata = new DeploymentMetadata("compositesinkgroup", 2);
		List<SimpleModule> modules = new ArrayList<SimpleModule>();
		modules.add(new SimpleModule(processor1Definition, metadata));
		modules.add(new SimpleModule(processor2Definition, metadata));
		modules.add(new SimpleModule(sinkDefinition, metadata));
		CompositeModule module = new CompositeModule("compositesink", ModuleType.sink, modules, metadata);
		assertEquals(sink, module.getType());
	}

}
