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
import static org.junit.Assert.assertNull;

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
		ModuleDefinition def = registry.findDefinition("file", "source");
		Assert.assertNotNull(def);
		Assert.assertTrue(def.getResource() instanceof ClassPathResource);

		def = registry.findDefinition("sink", "sink");
		Assert.assertNotNull(def);
		Assert.assertTrue(def.getResource() instanceof ClassPathResource);
	}

	@Test
	public void testNotFound() {
		ModuleDefinition def = registry.findDefinition("foo", "bar");
		assertNull(def);
	}

	@Test
	public void testShadowing() {
		ModuleDefinition def = registry.findDefinition("file", "source");
		Assert.assertNotNull(def);
		Assert.assertTrue(def.getResource() instanceof ClassPathResource);

		List<ModuleDefinition> multiple = registry.findDefinitions("file");
		assertEquals(1, multiple.size());
		assertEquals("source", multiple.get(0).getType());
	}


	@Test
	public void testFindSource() {
		List<ModuleDefinition> definitions = registry.findDefinitions(ModuleType.SOURCE);
		Assert.assertNotNull("A result list should always be returned", definitions);
		Assert.assertEquals(2, definitions.size());
		ArrayList<String> moduleNames = new ArrayList<String>();
		for (ModuleDefinition definition : definitions) {
			moduleNames.add(definition.getName());
		}
		Assert.assertTrue("File Source should be available",
				moduleNames.contains("file"));

	}

	@Test
	public void testFindSink() {
		List<ModuleDefinition> definitions = registry.findDefinitions(ModuleType.SINK);
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
		Assert.assertEquals(3, definitions.size());
		ArrayList<String> moduleNames = new ArrayList<String>();
		for (ModuleDefinition definition : definitions) {
			moduleNames.add(definition.getName());
		}
		Assert.assertTrue("File Source should be available",
				moduleNames.contains("file"));
		Assert.assertTrue(" Source should be available",
				moduleNames.contains("source"));
		Assert.assertTrue(" Source should be available",
				moduleNames.contains("sink"));


	}
}
