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

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.xd.module.ModuleDefinition;


/**
 * 
 * @author Eric Bottard
 */
public class CompositeModuleRegistryTests {

	private ModuleRegistry registry;

	@Before
	public void setup() {
		ClasspathTestModuleRegistry cp = new ClasspathTestModuleRegistry();
		FileModuleRegistry file = new FileModuleRegistry("../modules");
		file.setResourceLoader(new PathMatchingResourcePatternResolver());
		registry = new CompositeModuleRegistry(cp, file);
	}

	@Test
	public void testFound() {
		ModuleDefinition def = registry.lookup("http", "source");
		Assert.assertNotNull(def);
		Assert.assertTrue(def.getResource() instanceof FileSystemResource);

		def = registry.lookup("sink", "sink");
		Assert.assertNotNull(def);
		Assert.assertTrue(def.getResource() instanceof ClassPathResource);
	}

	@Test
	public void testNotFound() {
		ModuleDefinition def = registry.lookup("foo", "bar");
		assertNull(def);
	}

	@Test
	public void testShadowing() {
		ModuleDefinition def = registry.lookup("file", "source");
		Assert.assertNotNull(def);
		Assert.assertTrue(def.getResource() instanceof ClassPathResource);

		List<ModuleDefinition> multiple = registry.findDefinitions("file");
		assertEquals(2, multiple.size());
		assertEquals("source", multiple.get(0).getType());
		assertEquals("sink", multiple.get(1).getType());
	}

}
