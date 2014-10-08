/*
 * Copyright 2013-2014 the original author or authors.
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

import static org.junit.Assert.*;
import static org.springframework.xd.module.ModuleType.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.validation.BindException;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.CompositeModule;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.ModuleFactory;
import org.springframework.xd.module.options.ModuleOptionsMetadata;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;
import org.springframework.xd.module.options.PassthruModuleOptionsMetadata;

/**
 * @author Mark Fisher
 * @author David Turanski
 */
public class CompositeModuleTests {

	private volatile ModuleRegistry moduleRegistry;

	private volatile ModuleDefinition sourceDefinition;

	private volatile ModuleDefinition processor1Definition;

	private volatile ModuleDefinition processor2Definition;

	private volatile ModuleDefinition sinkDefinition;

	private final ModuleDeploymentProperties deploymentProperties = new ModuleDeploymentProperties();

	private ModuleFactory moduleFactory = new ModuleFactory(new ModuleOptionsMetadataResolver(){
		@Override
		public ModuleOptionsMetadata resolve(ModuleDefinition moduleDefinition) {
			return new PassthruModuleOptionsMetadata();
		}
	});


	@Before
	public void setupModuleDefinitions() throws BindException {
		moduleRegistry = new ResourceModuleRegistry("file:src/test/resources/testmodules/");
		sourceDefinition = moduleRegistry.findDefinition("source", source);
		processor1Definition = moduleRegistry.findDefinition("testprocessor1", processor);
		processor2Definition = moduleRegistry.findDefinition("testprocessor2", processor);
		sinkDefinition = moduleRegistry.findDefinition("sink", sink);
	}

	@Test
	public void testCompositeSource() {
		ModuleDescriptor sourceDescriptor = new ModuleDescriptor.Builder()
				.setModuleDefinition(sourceDefinition).setGroup("compositesourcegroup").setIndex(0).build();
		ModuleDescriptor processor1Descriptor = new ModuleDescriptor.Builder()
				.setModuleDefinition(processor1Definition).setGroup("compositesourcegroup").setIndex(1).build();
		ModuleDescriptor processor2Descriptor = new ModuleDescriptor.Builder()
				.setModuleDefinition(processor2Definition).setGroup("compositesourcegroup").setIndex(2).build();

		//parser results being reversed, we emulate here
		List<ModuleDescriptor> children = Arrays.asList(processor2Descriptor, processor1Descriptor, sourceDescriptor);
		ModuleDescriptor compositeDescriptor = new ModuleDescriptor.Builder()
				.setModuleDefinition(new ModuleDefinition("compositesource", ModuleType.source))
				.setGroup("compositesourcegroup")
				.addChildren(children)
				.build();
		Module module = moduleFactory.newInstance(compositeDescriptor, deploymentProperties);
		assertTrue(module instanceof CompositeModule);
		assertEquals(source, module.getType());
	}

	@Test
	public void testCompositeProcessor() {
		ModuleDescriptor processor1Descriptor = new ModuleDescriptor.Builder()
				.setModuleDefinition(processor1Definition)
				.setGroup("compositeprocessorgroup")
				.build();
		ModuleDescriptor processor2Descriptor = new ModuleDescriptor.Builder()
				.setModuleDefinition(processor2Definition)
				.setGroup("compositeprocessorgroup")
				.build();

		//parser results being reversed, we emulate here
		List<ModuleDescriptor> children = Arrays.asList(processor2Descriptor, processor1Descriptor);

		ModuleDescriptor compositeDescriptor = new ModuleDescriptor.Builder()
				.setModuleDefinition(new ModuleDefinition("compositeprocessor", ModuleType.processor))
				.setGroup("compositeprocessorgroup")
				.addChildren(children)
				.build();

		Module module = moduleFactory.newInstance(compositeDescriptor, deploymentProperties);
		assertTrue(module instanceof CompositeModule);
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
		ModuleDescriptor processor1Descriptor = new ModuleDescriptor.Builder()
				.setModuleDefinition(processor1Definition)
				.setGroup("compositesinkgroup")
				.build();
		ModuleDescriptor processor2Descriptor = new ModuleDescriptor.Builder()
				.setModuleDefinition(processor2Definition)
				.setGroup("compositesinkgroup")
				.build();
		ModuleDescriptor sinkDescriptor = new ModuleDescriptor.Builder()
				.setModuleDefinition(sinkDefinition)
				.setGroup("compositesinkgroup")
				.build();

		//parser results being reversed, we emulate here
		List<ModuleDescriptor> children = Arrays.asList(sinkDescriptor, processor2Descriptor, processor1Descriptor);
		ModuleDescriptor compositeDescriptor = new ModuleDescriptor.Builder()
				.setModuleDefinition(new ModuleDefinition("compositesink", ModuleType.sink))
				.setGroup("compositesinkgroup")
				.addChildren(children)
				.setIndex(2)
				.build();
		Module module = moduleFactory.newInstance(compositeDescriptor, deploymentProperties);
		assertTrue(module instanceof CompositeModule);
		assertEquals(sink, module.getType());
	}

}
