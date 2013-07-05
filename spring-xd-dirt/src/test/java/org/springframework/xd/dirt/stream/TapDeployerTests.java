/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author David Turanski
 *
 */
public class TapDeployerTests {
	TapDefinitionRepository repository; 
	StreamDefinitionRepository streamRepository; 
	SubscribableChannel outputChannel; 
	TapDeploymentMessageSender sender;
	TapDeployer tapDeployer;
	@Before
	public void setUp() {
		repository = mock(TapDefinitionRepository.class);
		streamRepository = mock(StreamDefinitionRepository.class);
		outputChannel = new DirectChannel();
		sender = new TapDeploymentMessageSender(outputChannel);
		tapDeployer = new TapDeployer(repository,streamRepository, sender);
	}
	@Test
	public void testCreateSucceeds() {
		TapDefinition tapDefinition = new TapDefinition("tap1", "test", "tap @test | file");
		when(repository.save(tapDefinition)).thenReturn(tapDefinition);
		when(streamRepository.exists("test")).thenReturn(true);
		tapDeployer.create(tapDefinition);
		verify(repository).save(tapDefinition);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateFailsIfSourceStreamDoesNotExist() {
		TapDefinition tapDefinition = new TapDefinition("tap1", "test", "tap @test | file");
		when(repository.save(tapDefinition)).thenReturn(tapDefinition);
		when(streamRepository.exists("test")).thenReturn(false);
		tapDeployer.create(tapDefinition);
	}

	@Test
	public void testDeploySucceeds() {
		TapDefinition tapDefinition = new TapDefinition("tap1", "test", "tap @test | file");
		when(repository.findOne("tap1")).thenReturn(tapDefinition);
		final AtomicInteger messageCount = new AtomicInteger();
		outputChannel.subscribe(new MessageHandler(){
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				messageCount.getAndIncrement();
			}});

		tapDeployer.deploy("tap1");
		assertEquals(2,messageCount.get());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testDeployFails() {
		tapDeployer.deploy("tap1");
	}
}
