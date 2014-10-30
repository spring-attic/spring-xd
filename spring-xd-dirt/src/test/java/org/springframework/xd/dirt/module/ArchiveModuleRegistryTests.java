/*
 *
 *  * Copyright 2011-2014 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.xd.dirt.module;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;

/**
 * Tests for ArchiveModuleRegistry.
 *
 * @author Eric Bottard
 */
public class ArchiveModuleRegistryTests {

	private ArchiveModuleRegistry registry = new ArchiveModuleRegistry("file:src/test/resources/ArchiveModuleRegistryTests-modules/");

	@Test(expected = IllegalStateException.class)
	public void cantHaveBothJarFileAndDir() {
		registry.findDefinition("dummy", ModuleType.processor);
	}

	@Test
	public void jarFilesModulesDontIncludeExtensionInTheirName() {
		List<ModuleDefinition> definitions = registry.findDefinitions(ModuleType.sink);
		assertThat(definitions, contains(ResourceModuleRegistryTests.hasName("module-zipped")));
	}

	@Test
	public void beingADirHasPriorityOverEndingInDotJar() {
		List<ModuleDefinition> definitions = registry.findDefinitions(ModuleType.source);
		assertThat(definitions, contains(ResourceModuleRegistryTests.hasName("i-am-a-valid-module.jar")));
	}
}
