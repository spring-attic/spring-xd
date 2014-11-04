/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.module;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import org.springframework.xd.dirt.module.support.SingletonModuleRegistry;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.SimpleModuleDefinition;


/**
 * Tests that {@link SingletonModuleRegistry} behaves as expected with well known
 * {@link org.springframework.core.io.Resource} abstractions.
 *
 * @author David Turanski
 */
public class SingletonModuleRegistryTests {

	final static String MODULE_NAME = "aModule";

	@Test
	public void testDefinitionLookupForConfiguredModuleType() {
		ModuleType[] moduleTypes = new ModuleType[] {ModuleType.job, ModuleType.processor, ModuleType.sink,
				ModuleType.source};
		for (ModuleType moduleType : moduleTypes) {
			ModuleRegistry registry = new SingletonModuleRegistry(moduleType, MODULE_NAME);
			SimpleModuleDefinition def = (SimpleModuleDefinition)registry.findDefinition(MODULE_NAME, moduleType);
			assertNotNull(def);
			assertEquals(MODULE_NAME, def.getName());
			assertEquals(moduleType, def.getType());
			assertEquals("",def.getLocation());
		}
	}

	@Test
	public void testDefinitionLookupWithDifferentModuleType() {
		ModuleRegistry registry = new SingletonModuleRegistry(ModuleType.source, MODULE_NAME);
		ModuleDefinition def = registry.findDefinition(MODULE_NAME, ModuleType.sink);
		assertNull(def);
	}

	@Test
	public void testDefinitionLookupWithDifferentModuleName() {
		ModuleRegistry registry = new SingletonModuleRegistry(ModuleType.source, MODULE_NAME);
		ModuleDefinition def = registry.findDefinition("foo", ModuleType.sink);
		assertNull(def);
	}

	@Test
	public void testDefinitionLookupWithWildCardModuleType() {
		ModuleRegistry registry = new SingletonModuleRegistry(ModuleType.source, MODULE_NAME);
		List<ModuleDefinition> defs = registry.findDefinitions(MODULE_NAME);
		assertNotNull(defs);
		assertEquals(1, defs.size());
		ModuleDefinition def = defs.get(0);
		assertEquals(MODULE_NAME, def.getName());
		assertEquals(ModuleType.source, def.getType());
	}

	@Test
	public void testDefinitionLookupWithWildCardModuleName() {
		ModuleRegistry registry = new SingletonModuleRegistry(ModuleType.source, MODULE_NAME);
		List<ModuleDefinition> defs = registry.findDefinitions(ModuleType.source);
		assertNotNull(defs);
		assertEquals(1, defs.size());
		ModuleDefinition def = defs.get(0);
		assertEquals(MODULE_NAME, def.getName());
		assertEquals(ModuleType.source, def.getType());
	}

	@Test
	public void testDefinitionLookupWithWildCards() {
		ModuleRegistry registry = new SingletonModuleRegistry(ModuleType.source, MODULE_NAME);
		List<ModuleDefinition> defs = registry.findDefinitions();
		assertNotNull(defs);
		assertEquals(1, defs.size());
		ModuleDefinition def = defs.get(0);
		assertEquals(MODULE_NAME, def.getName());
		assertEquals(ModuleType.source, def.getType());
	}

}
