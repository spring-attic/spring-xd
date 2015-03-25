/*
 * Copyright 2014-2015 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.module.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import org.springframework.messaging.MessageChannel;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDefinitions;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.options.DefaultModuleOptionsMetadataResolver;

/**
 * @author David Turanski
 */
public class ModuleFactoryTests {
	private ModuleFactory moduleFactory = new ModuleFactory(new DefaultModuleOptionsMetadataResolver());

	@Test
	public void createJavaConfiguredModule() {
		ModuleDefinition moduleDefinition = ModuleDefinitions.simple("siDslModule", ModuleType.processor,
				"classpath:/ModuleFactoryTests/modules/processor/siDslModule.jar");
		ModuleDescriptor moduleDescriptor = new ModuleDescriptor.Builder()
				.setModuleDefinition(moduleDefinition)
				.setModuleName("javaConfigModule")
				.setGroup("group")
				.setModuleDefinition(moduleDefinition)
				.setParameter("prefix", "foo")
				.build();

		Module module = moduleFactory.createModule(moduleDescriptor, new ModuleDeploymentProperties());
		assertTrue(module instanceof JavaConfiguredModule);
		module.initialize();
		module.getComponent("input", MessageChannel.class);
		module.getComponent("output", MessageChannel.class);
		module.getComponent("transformer", Object.class);
	}

	@Test
	public void invalidJavaConfiguredModuleShouldThrowException() {
		ModuleDefinition moduleDefinition = ModuleDefinitions.simple("badModule", ModuleType.processor,
				"classpath:/ModuleFactoryTests/modules/processor/badModule.jar");
		ModuleDescriptor moduleDescriptor = new ModuleDescriptor.Builder()
				.setModuleDefinition(moduleDefinition)
				.setModuleName("badModule")
				.setGroup("group")
				.setModuleDefinition(moduleDefinition)
				.setParameter("prefix", "foo")
				.build();

		Module module = moduleFactory.createModule(moduleDescriptor, new ModuleDeploymentProperties());
		try {
			module.initialize();
			fail("should throw exception");
		}
		catch (RuntimeException e) {
			assertEquals("Unable to find a module @Configuration class in base_packages: foooo.com.acme for module " +
					"badModule:processor", e.getMessage());
		}
	}

	@Test
	public void createXmlModule() {
		createResourceConfiguredModule("xmlModule", ModuleType.processor);
	}

	@Test(expected = IllegalStateException.class)
	public void multipleXmlConfigThrowsException() {
		createResourceConfiguredModule("invalid", ModuleType.processor);
	}

	@Test(expected = IllegalArgumentException.class)
	public void missingConfigThrowsException() {
		ModuleDefinition moduleDefinition = ModuleDefinitions.simple("missing", ModuleType.processor,
				"classpath:/ModuleFactoryTests/modules/processor/missing");
		createResourceConfiguredModule("missing", ModuleType.processor);
	}


	@Test
	public void createGroovyModule() {
		createResourceConfiguredModule("groovyModule", ModuleType.processor);
	}

	private void createResourceConfiguredModule(String moduleName, ModuleType moduleType) {
		ModuleDefinition moduleDefinition = ModuleDefinitions.simple(moduleName, moduleType,
				"classpath:/ModuleFactoryTests/modules/" + moduleType + "/" + moduleName + "/");
		ModuleDescriptor moduleDescriptor = new ModuleDescriptor.Builder()
				.setModuleDefinition(moduleDefinition)
				.setModuleName(moduleName)
				.setGroup("group")
				.setParameter("bar", "hello")
				.build();

		Module module = moduleFactory.createModule(moduleDescriptor, new ModuleDeploymentProperties());
		assertTrue(module instanceof ResourceConfiguredModule);
		module.initialize();
		assertEquals("foo", module.getComponent("foo", String.class));
		assertEquals("hello", module.getComponent("bar", String.class));
	}
}
