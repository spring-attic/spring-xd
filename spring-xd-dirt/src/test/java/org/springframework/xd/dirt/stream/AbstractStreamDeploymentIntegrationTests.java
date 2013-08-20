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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.interceptor.WireTap;


/**
 * @author Mark Fisher
 */
public abstract class AbstractStreamDeploymentIntegrationTests {

	private AbstractApplicationContext context;

	private StreamDefinitionRepository streamDefinitionRepository;

	private StreamRepository streamRepository;

	private StreamDeployer streamDeployer;

	private final QueueChannel tapChannel = new QueueChannel();

	@Before
	public final void setUp() {
		String transport = this.getTransport();
		System.setProperty("xd.home", "..");
		System.setProperty("xd.transport", transport);
		System.setProperty("xd.analytics", "memory");
		System.setProperty("xd.store", "memory");
		this.context = new ClassPathXmlApplicationContext(
				"META-INF/spring-xd/internal/container.xml",
				"META-INF/spring-xd/internal/deployers.xml",
				"META-INF/spring-xd/plugins/streams.xml",
				"META-INF/spring-xd/store/memory-admin.xml",
				"META-INF/spring-xd/transports/" + transport + "-admin.xml");
		this.streamDefinitionRepository = context.getBean(StreamDefinitionRepository.class);
		this.streamRepository = context.getBean(StreamRepository.class);
		this.streamDeployer = context.getBean(StreamDeployer.class);
		AbstractMessageChannel deployChannel = context.getBean("deployChannel", AbstractMessageChannel.class);
		AbstractMessageChannel undeployChannel = context.getBean("undeployChannel", AbstractMessageChannel.class);
		deployChannel.addInterceptor(new WireTap(tapChannel));
		undeployChannel.addInterceptor(new WireTap(tapChannel));
		setupApplicationContext(context);
	}

	@After
	public final void shutDown() {
		this.cleanup(this.context);
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public final void deployAndUndeploy() {
		assertEquals(0, streamRepository.count());
		StreamDefinition definition = new StreamDefinition("test1", "time | log");
		streamDefinitionRepository.save(definition);
		streamDeployer.deploy("test1");
		assertEquals(1, streamRepository.count());
		assertTrue(streamRepository.exists("test1"));
		streamDeployer.undeploy("test1");
		assertEquals(0, streamRepository.count());
		assertFalse(streamRepository.exists("test1"));
		assertModuleRequest("log", false);
		assertModuleRequest("time", false);
		assertModuleRequest("time", true);
		assertModuleRequest("log", true);
		assertNull(tapChannel.receive(0));
	}

	private void assertModuleRequest(String moduleName, boolean remove) {
		Message<?> next = tapChannel.receive(0);
		assertNotNull(next);
		String payload = (String) next.getPayload();
		assertTrue(payload.contains("\"module\":\"" + moduleName + "\""));
		assertTrue(payload.contains("\"remove\":" + (remove ? "true" : "false")));
	}

	protected void setupApplicationContext(ApplicationContext context) {
	}

	protected abstract String getTransport();

	protected abstract void cleanup(ApplicationContext context);

}
