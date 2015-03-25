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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.xd.module.ModuleType.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.TestModuleDefinitions;

/**
 * Tests for ResourceModuleRegistry implementation logic.
 *
 * @author Eric Bottard
 */
public class WritableResourceModuleRegistryLogicTests {

	private ModuleRegistry registry = new ResourceModuleRegistry("file:src/test/resources/ResourceModuleRegistryLogicTests-modules/");

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Test(expected = IllegalStateException.class)
	public void cantHaveBothJarFileAndDir() {
		registry.findDefinition("dummy", ModuleType.processor);
	}

	@Test
	public void jarFilesModulesDontIncludeExtensionInTheirName() {
		List<ModuleDefinition> definitions = registry.findDefinitions(ModuleType.sink);
		assertThat(definitions, not(contains(ResourceModuleRegistryTests.hasName("module-zipped.jar"))));
		assertThat(definitions, contains(ResourceModuleRegistryTests.hasName("module-zipped")));
	}

	@Test
	public void beingAJarHasPriorityOverBeingADir() {
		List<ModuleDefinition> definitions = registry.findDefinitions(ModuleType.source);
		assertThat(definitions, contains(ResourceModuleRegistryTests.hasName("i-am-a-valid-module")));
	}

	@Test
	public void testDeleteAsJarFile() throws IOException {
		WritableModuleRegistry writableModuleRegistry = new WritableResourceModuleRegistry(tempPath());
		File processors = temp.newFolder("processor");
		org.springframework.util.Assert.isTrue(new File(processors, "foo.jar").createNewFile(), "could not create dummy file");


		org.springframework.util.Assert.isTrue(writableModuleRegistry.delete(TestModuleDefinitions.dummy("foo", processor)));
	}

	private String tempPath() {
		return "file:" + temp.getRoot().getAbsolutePath();
	}
}
