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

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.validation.BindException;
import org.springframework.xd.dirt.core.ModuleDescriptor;
import org.springframework.xd.dirt.event.AbstractModuleEvent;
import org.springframework.xd.dirt.module.ModuleDeployer;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.module.TestModuleEventListener;
import org.springframework.xd.dirt.server.options.XDPropertyKeys;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.SimpleModule;
import org.springframework.xd.module.options.ModuleOptions;
import org.springframework.xd.module.options.SimpleModuleOptionsMetadata;
import org.springframework.xd.module.support.ParentLastURLClassLoader;

/**
 * Integration test that deploys a few simple test modules to verify the full functionality of {@link StreamPlugin}
 * 
 * @author Jennifer Hickey
 * @author David Turanski
 */
public class StreamPluginModuleDeploymentTests {

	MessageBus bus;

	private ModuleDeployer moduleDeployer;

	private TestModuleEventListener eventListener;

	private SimpleModule source;

	private SimpleModule sink;

	private ClassLoader parentClassLoader;

	@Before
	public void setup() {
		GenericApplicationContext child = new GenericApplicationContext();
		XmlBeanDefinitionReader childReader = new XmlBeanDefinitionReader(child);
		childReader.loadBeanDefinitions(new ClassPathResource(
				"org/springframework/xd/dirt/plugins/stream/module-deployer-context.xml"));

		GenericApplicationContext parent = new GenericApplicationContext();
		Properties xdProperties = new Properties();
		xdProperties.setProperty(XDPropertyKeys.XD_TRANSPORT, "local");
		xdProperties.setProperty(XDPropertyKeys.XD_ANALYTICS, "memory");
		xdProperties.setProperty(XDPropertyKeys.XD_HOME, new File("..").getAbsolutePath());

		parent.getEnvironment().getPropertySources().addFirst(
				new PropertiesPropertySource("xdProperties", xdProperties));
		XmlBeanDefinitionReader parentReader = new XmlBeanDefinitionReader(parent);
		parentReader.loadBeanDefinitions(new ClassPathResource(
				"org/springframework/xd/dirt/plugins/stream/StreamPluginModuleDeploymentTests-context.xml"));

		parent.refresh();
		parentClassLoader = parent.getClassLoader();
		child.setParent(parent);
		child.refresh();

		this.moduleDeployer = child.getBean(ModuleDeployer.class);
		this.bus = parent.getBean(MessageBus.class);
		this.eventListener = child.getBean(TestModuleEventListener.class);
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
	 * Validates that channels defined in the modules end up in the shared {@link MessageBus}
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void moduleChannelsRegisteredWithSameMessageBus() throws InterruptedException {
		this.source = sendModuleRequest(createSourceModuleRequest());
		assertEquals(2, getBindings(bus).size());
		sendModuleRequest(createSinkModuleRequest());
		assertEquals(3, getBindings(bus).size());
		getBindings(bus).clear();
	}

	@Test
	public void moduleUndeployUnregistersChannels() throws InterruptedException, BindException {
		ModuleDescriptor moduleDescriptor = createSourceModuleDescriptor();
		ModuleOptions moduleOptions = new SimpleModuleOptionsMetadata().interpolate(Collections.<String, String> emptyMap());
		moduleDeployer.deployAndStore(createSimpleModule(moduleDescriptor, moduleOptions), moduleDescriptor);
		assertEquals(2, getBindings(bus).size());
		moduleDeployer.undeploy(moduleDescriptor);
		assertEquals(0, getBindings(bus).size());
	}

	// todo: refactor this now that ZooKeeper is used for deployment
	private SimpleModule sendModuleRequest(ModuleDeploymentRequest request) throws InterruptedException {
		// Message<?> message = MessageBuilder.withPayload(request.toString()).build();
		// moduleDeployer.handleMessage(message);
		AbstractModuleEvent moduleDeployedEvent = eventListener.getEvents().poll(5, TimeUnit.SECONDS);
		assertNotNull(moduleDeployedEvent);
		return (SimpleModule) moduleDeployedEvent.getSource();
	}

	private ModuleDeploymentRequest createSourceModuleRequest() {
		ModuleDeploymentRequest request = new ModuleDeploymentRequest();
		request.setGroup("test");
		request.setType(ModuleType.source);
		request.setModule("source");
		request.setIndex(0);
		return request;
	}

	private ModuleDescriptor createSourceModuleDescriptor() {
		ModuleDefinition definition = new ModuleDefinition("source", ModuleType.source);
		return new ModuleDescriptor(definition, "test", definition.getName() + "-0", 0, null, 1);
	}

	// todo: this is copied from ContainerRegistrar where it's private
	private Module createSimpleModule(ModuleDescriptor descriptor, ModuleOptions moduleOptions) {
		String streamName = descriptor.getStreamName();
		int index = descriptor.getIndex();
		String sourceChannelName = descriptor.getSourceChannelName();
		String sinkChannelName = descriptor.getSinkChannelName();
		DeploymentMetadata metadata = new DeploymentMetadata(streamName, index, sourceChannelName, sinkChannelName);
		ModuleDefinition definition = descriptor.getModuleDefinition();
		ClassLoader classLoader = (definition.getClasspath() == null) ? null
				: new ParentLastURLClassLoader(definition.getClasspath(), parentClassLoader);
		Module module = new SimpleModule(definition, metadata, classLoader, moduleOptions);
		return module;
	}

	private ModuleDeploymentRequest createSinkModuleRequest() {
		ModuleDeploymentRequest request = new ModuleDeploymentRequest();
		request.setGroup("test");
		request.setType(ModuleType.sink);
		request.setModule("sink");
		request.setIndex(1);
		return request;
	}

	private Collection<?> getBindings(MessageBus bus) {
		DirectFieldAccessor accessor = new DirectFieldAccessor(bus);
		return (List<?>) accessor.getPropertyValue("bindings");
	}

}
