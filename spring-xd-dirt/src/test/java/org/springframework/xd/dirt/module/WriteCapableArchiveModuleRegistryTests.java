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

import static org.springframework.xd.module.ModuleType.processor;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.util.Assert;
import org.springframework.xd.module.ModuleDefinitions;

public class WriteCapableArchiveModuleRegistryTests {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void testDeleteAsDirectory() throws IOException {
		WriteCapableArchiveModuleRegistry registry = new WriteCapableArchiveModuleRegistry(tempPath());
		temp.newFolder("processor", "foo", "some", "garbage", "inside");

		Assert.isTrue(registry.delete(ModuleDefinitions.dummy("foo", processor)));
	}

	@Test
	public void testDeleteAsJarFile() throws IOException {
		WriteCapableArchiveModuleRegistry registry = new WriteCapableArchiveModuleRegistry(tempPath());
		File processors = temp.newFolder("processor");
		Assert.isTrue(new File(processors, "foo.jar").createNewFile(), "could not create dummy file");


		Assert.isTrue(registry.delete(ModuleDefinitions.dummy("foo", processor)));
	}

	private String tempPath() {
		return "file:" + temp.getRoot().getAbsolutePath();
	}

}