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


import static org.junit.Assert.assertNull;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;


/**
 * 
 * @author Eric Bottard
 * @author Glenn Renfro
 */
public class CompositeModuleRegistryTests {

	private ModuleRegistry registry;

	@Before
	public void setup() {
		ClasspathTestModuleRegistry cp = new ClasspathTestModuleRegistry();
		FileModuleRegistry file = new FileModuleRegistry("src/test/resources/testmodules");
		file.setResourceLoader(new PathMatchingResourcePatternResolver());
		registry = new CompositeModuleRegistry(cp, file);
	}

	@Test
	public void testFound() {
		ModuleDefinition def = registry.findDefinition("file", ModuleType.source);
		Assert.assertNotNull(def);
		Assert.assertTrue(def.getResource() instanceof ClassPathResource);

		def = registry.findDefinition("sink", ModuleType.sink);
		Assert.assertNotNull(def);
		Assert.assertTrue(def.getResource() instanceof ClassPathResource);
	}

	@Test
	public void testNotFound() {
		ModuleDefinition def = registry.findDefinition("foo", ModuleType.sink);
		assertNull(def);
	}

	@Test
	public void testFindSource() {
		List<ModuleDefinition> definitions = registry.findDefinitions(ModuleType.source);
		Assert.assertNotNull("A result list should always be returned", definitions);
		Assert.assertEquals(4, definitions.size());
		ArrayList<String> moduleNames = new ArrayList<String>();
		for (ModuleDefinition definition : definitions) {
			moduleNames.add(definition.getName());
		}
		Assert.assertTrue("File Source should be available",
				moduleNames.contains("file"));

	}

	@Test
	public void testFindSink() {
		List<ModuleDefinition> definitions = registry.findDefinitions(ModuleType.sink);
		Assert.assertNotNull("A result list should always be returned", definitions);
		Assert.assertEquals(1, definitions.size());
		ArrayList<String> moduleNames = new ArrayList<String>();
		for (ModuleDefinition definition : definitions) {
			moduleNames.add(definition.getName());
		}
		Assert.assertTrue("Sink Sink should be available",
				moduleNames.contains("sink"));

	}

	@Test
	public void testFindAll() {
		List<ModuleDefinition> definitions = registry.findDefinitions();
		Assert.assertNotNull("A result list should always be returned", definitions);
		Assert.assertEquals(7, definitions.size());
		ArrayList<String> moduleNames = new ArrayList<String>();
		for (ModuleDefinition definition : definitions) {
			moduleNames.add(definition.getName());
		}
		Assert.assertTrue("File Source should be available",
				moduleNames.contains("file"));
		Assert.assertTrue(" Source should be available",
				moduleNames.contains("source"));
		Assert.assertTrue(" Sink should be available",
				moduleNames.contains("sink"));
		Assert.assertTrue(" source-config should be available",
				moduleNames.contains("source-config"));
	}

	@Test
	public void testClassPath() {
		List<ModuleDefinition> definitions = registry.findDefinitions();
		String sourceConfigClassPath = null;
		URL[] sourceConfigNoLib = null;

		ModuleDefinition fileModule = null;

		for (ModuleDefinition definition : definitions) {
			if (definition.getName().equals("source-config") && definition.getClasspath().length == 1) {
				sourceConfigClassPath = definition.getClasspath()[0].toString();
			}
			if (definition.getName().equals("source-config-no-lib")) {
				sourceConfigNoLib = definition.getClasspath();
			}

			if (definition.getName().equals("file")) {
				fileModule = definition;
			}
		}
		Assert.assertNull("File Source should not have a associated jar", fileModule.getClasspath());
		Assert.assertNotNull("sourceConfigClassPath should not be null", sourceConfigClassPath);
		Assert.assertTrue("source-config classpath should end with ../lib/source-config.jar",
				sourceConfigClassPath.endsWith("../lib/source-config.jar"));
		Assert.assertNull("source-config-no-lib should have no associated libraries", sourceConfigNoLib);

	}
}
