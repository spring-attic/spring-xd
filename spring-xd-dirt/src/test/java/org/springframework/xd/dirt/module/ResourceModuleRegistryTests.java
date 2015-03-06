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

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.core.io.Resource;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;


/**
 * Tests that {@link WritableResourceModuleRegistry} behaves as expected with well known {@link Resource} abstractions.
 * 
 * @author Eric Bottard
 */
@RunWith(Parameterized.class)
public class ResourceModuleRegistryTests {

	private static final String TRANSFORM_MODULE_NAME = "transform-test";

	private static final String FILTER_MODULE_NAME = "filter-test";

	private static final String FILE_MODULE_NAME = "file-test";

	private ResourceModuleRegistry registry;

	@Parameters(name = "{0}")
	public static Iterable<Object[]> roots() throws MalformedURLException {
		List<Object[]> result = new ArrayList<Object[]>();
		result.add(new Object[] { "file:src/test/resources/ResourceModuleRegistryTests-modules/" });
		result.add(new Object[] { "classpath:/ResourceModuleRegistryTests-modules/" });
		result.add(new Object[] { "classpath*:/ResourceModuleRegistryTests-modules/" });
		return result;
	}

	public ResourceModuleRegistryTests(String root) {
		registry = new ResourceModuleRegistry(root);
	}

	@Test
	public void testSingleDefinitionLookup() {
		ModuleDefinition def = registry.findDefinition(FILE_MODULE_NAME, ModuleType.sink);
		assertNotNull(def);
		assertEquals(FILE_MODULE_NAME, def.getName());
		assertEquals(ModuleType.sink, def.getType());
	}

	@Test
	public void testLookupSameNameDifferentTypes() {
		List<ModuleDefinition> matches = registry.findDefinitions(FILE_MODULE_NAME);
		assertThat(matches, hasItem(hasType(ModuleType.source)));
		assertThat(matches, hasItem(hasType(ModuleType.sink)));
		assertThat(matches, everyItem(hasName(FILE_MODULE_NAME)));
	}

	@Test
	public void testLookupByType() {
		List<ModuleDefinition> matches = registry.findDefinitions(ModuleType.processor);
		assertThat(matches, hasItem(hasName(FILTER_MODULE_NAME)));
		assertThat(matches, hasItem(hasName(TRANSFORM_MODULE_NAME)));
		assertThat(matches, everyItem(hasType(ModuleType.processor)));
	}

	@Test
	public void testLookupAll() {
		List<ModuleDefinition> matches = registry.findDefinitions();
		assertThat(matches, hasItem(both(hasName(FILTER_MODULE_NAME)).and(hasType(ModuleType.processor))));
		assertThat(matches, hasItem(both(hasName(FILE_MODULE_NAME)).and(hasType(ModuleType.source))));
		assertThat(matches, hasItem(both(hasName(FILE_MODULE_NAME)).and(hasType(ModuleType.sink))));
	}


	static Matcher<ModuleDefinition>  hasName(final String name) {
		return new CustomMatcher<ModuleDefinition>("Module named " + name) {

			@Override
			public boolean matches(Object item) {
				return ((ModuleDefinition) item).getName().equals(name);
			}
		};
	}

	static Matcher<ModuleDefinition> hasType(final ModuleType type) {
		return new CustomMatcher<ModuleDefinition>("Module with type " + type) {

			@Override
			public boolean matches(Object item) {
				return ((ModuleDefinition) item).getType() == type;
			}
		};
	}
}
