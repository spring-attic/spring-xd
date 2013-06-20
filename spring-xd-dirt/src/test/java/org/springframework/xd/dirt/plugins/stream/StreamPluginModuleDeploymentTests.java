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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.x.channel.registry.ChannelRegistry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.dirt.event.AbstractModuleEvent;
import org.springframework.xd.dirt.module.ModuleDeployer;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.module.ModuleEventListener;
import org.springframework.xd.module.SimpleModule;

/**
 * Integration test that deploys a few simple test modules to verify the full
 * functionality of {@link StreamPlugin}
 *
 * @author Jennifer Hickey
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class StreamPluginModuleDeploymentTests {

	@Autowired
	private ModuleDeployer moduleDeployer;

	@Autowired
	private ModuleEventListener eventListener;

	private SimpleModule source;

	private SimpleModule sink;

	@BeforeClass
	public static void setContextProperties() {
		System.setProperty("xd.transport", "local");
	}

	@After
	public void tearDown() {
		if (source != null) {
			source.stop();
		}
		if (sink != null) {
			sink.stop();
		}
	}

	/**
	 * Validates that channels defined in the modules end up in the shared
	 * {@link ChannelRegistry}
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void moduleChannelsRegisteredWithSameRegistry() throws InterruptedException {
		this.source = sendModuleRequest(createSourceModuleRequest());
		ChannelRegistry registry = source.getApplicationContext().getBean(ChannelRegistry.class);
		assertEquals(1, getBridges(registry).size());
		this.sink = sendModuleRequest(createSinkModuleRequest());
		assertSame(registry, sink.getApplicationContext().getBean(ChannelRegistry.class));
		assertEquals(2, getBridges(registry).size());
	}

	@Test
	public void moduleUndeployUnregistersChannels() throws InterruptedException {
		ModuleDeploymentRequest request = createSourceModuleRequest();
		SimpleModule module = sendModuleRequest(request);
		ChannelRegistry registry = module.getApplicationContext().getBean(ChannelRegistry.class);
		assertEquals(1, getBridges(registry).size());
		request.setRemove(true);
		sendModuleRequest(request);
		assertEquals(0, getBridges(registry).size());
	}

	private SimpleModule sendModuleRequest(ModuleDeploymentRequest request) throws InterruptedException {
		Message<?> message = MessageBuilder.withPayload(request.toString()).build();
		moduleDeployer.handleMessage(message);
		AbstractModuleEvent moduleDeployedEvent = eventListener.getEvents().poll(5, TimeUnit.SECONDS);
		assertNotNull(moduleDeployedEvent);
		return (SimpleModule) moduleDeployedEvent.getSource();
	}

	private ModuleDeploymentRequest createSourceModuleRequest() {
		ModuleDeploymentRequest request = new ModuleDeploymentRequest();
		request.setGroup("test");
		request.setType("source");
		request.setModule("source");
		request.setIndex(0);
		return request;
	}

	private ModuleDeploymentRequest createSinkModuleRequest() {
		ModuleDeploymentRequest request = new ModuleDeploymentRequest();
		request.setGroup("test");
		request.setType("sink");
		request.setModule("sink");
		request.setIndex(1);
		return request;
	}

	private Collection<?> getBridges(ChannelRegistry registry) {
		DirectFieldAccessor accessor = new DirectFieldAccessor(registry);
		List<?> bridges = (List<?>) accessor.getPropertyValue("bridges");
		return bridges;
	}
}
