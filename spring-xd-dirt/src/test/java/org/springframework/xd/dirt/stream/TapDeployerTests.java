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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.Resource;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.stream.memory.InMemoryStreamDefinitionRepository;
import org.springframework.xd.dirt.stream.memory.InMemoryTapDefinitionRepository;
import org.springframework.xd.dirt.stream.memory.InMemoryTapInstanceRepository;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;

/**
 * @author David Turanski
 * @author Gunnar Hillert
 * @author Glenn Renfro
 */
public class TapDeployerTests {

	private TapDefinitionRepository repository;

	private TapInstanceRepository tapInstanceRepository;

	private StreamDefinitionRepository streamDefinitionRepository;

	private SubscribableChannel deployChannel;

	private SubscribableChannel undeployChannel;

	private DeploymentMessageSender sender;

	private TapDeployer tapDeployer;

	@Before
	public void setUp() {
		repository = new InMemoryTapDefinitionRepository();
		tapInstanceRepository = new InMemoryTapInstanceRepository();
		streamDefinitionRepository = new InMemoryStreamDefinitionRepository();
		deployChannel = new DirectChannel();
		undeployChannel = new PublishSubscribeChannel();
		sender = new DeploymentMessageSender(deployChannel, undeployChannel);
		XDParser parser = new XDStreamParser(streamDefinitionRepository, moduleRegistry());
		tapDeployer = new TapDeployer(repository, streamDefinitionRepository, sender, parser, tapInstanceRepository);
	}

	@Test
	public void testCreateSucceeds() {
		TapDefinition tapDefinition = new TapDefinition("tap1", "test", "tap @test | file");
		streamDefinitionRepository.save(new StreamDefinition("test", "time | log"));
		tapDeployer.save(tapDefinition);
		assertTrue(repository.exists("tap1"));
	}

	@Test(expected = NoSuchDefinitionException.class)
	public void testCreateFailsIfSourceStreamDoesNotExist() {
		TapDefinition tapDefinition = new TapDefinition("tap1", "test", "tap @test | file");
		tapDeployer.save(tapDefinition);
	}

	@Test
	public void testDeploySucceeds() {
		TapDefinition tapDefinition = new TapDefinition("tap1", "test", "tap @test | file");
		repository.save(tapDefinition);
		final AtomicInteger messageCount = new AtomicInteger();
		deployChannel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				messageCount.getAndIncrement();
			}
		});
		tapDeployer.deploy("tap1");
		assertEquals(2, messageCount.get());
	}

	@Test
	public void testUndeploySucceeds() {
		TapDefinition tapDefinition = new TapDefinition("tap1", "test", "tap @test | file");
		repository.save(tapDefinition);
		final AtomicInteger deployCount = new AtomicInteger();
		final AtomicInteger undeployCount = new AtomicInteger();
		deployChannel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				deployCount.getAndIncrement();
			}
		});
		undeployChannel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				undeployCount.getAndIncrement();
			}
		});
		tapDeployer.deploy("tap1");
		tapDeployer.undeploy("tap1");
		assertEquals(2, deployCount.get());
		assertEquals(2, undeployCount.get());
	}

	@Test(expected = NoSuchDefinitionException.class)
	public void testDeployFailsForMissingDefinition() {
		tapDeployer.deploy("tap1");
	}

	@After
	public void clearRepos() {
		repository.deleteAll();
		streamDefinitionRepository.deleteAll();
	}

	public ModuleRegistry moduleRegistry() {
		ModuleRegistry registry = mock(ModuleRegistry.class);
		Resource resource = mock(Resource.class);
		ArrayList<ModuleDefinition> definitions = new ArrayList<ModuleDefinition>();
		definitions.add(new ModuleDefinition(ModuleType.TAP.getTypeName(), ModuleType.TAP.getTypeName(), resource));
		when(registry.findDefinitions(ModuleType.TAP.getTypeName())).thenReturn(definitions);

		definitions = new ArrayList<ModuleDefinition>();
		definitions.add(new ModuleDefinition(ModuleType.SINK.getTypeName(), ModuleType.SINK.getTypeName(), resource));
		when(registry.findDefinitions("file")).thenReturn(definitions);

		when(registry.lookup("file", ModuleType.SINK.getTypeName())).thenReturn(
				new ModuleDefinition(ModuleType.SINK.getTypeName(), ModuleType.SINK.getTypeName(), resource));
		when(registry.lookup("tap", ModuleType.SOURCE.getTypeName())).thenReturn(
				new ModuleDefinition(ModuleType.SOURCE.getTypeName(), ModuleType.SOURCE.getTypeName(), resource));

		return registry;
	}
}
