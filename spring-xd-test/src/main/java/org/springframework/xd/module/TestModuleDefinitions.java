/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.module;

import java.io.File;
import java.io.IOException;

import org.springframework.util.Assert;

/**
 * Utility class for creating ModuleDefinitions in the context of tests.
 *
 * @author Eric Bottard
 */
public class TestModuleDefinitions {
	/**
	 * Create a new definition for a dummy module, for testing purposes only. The resulting module is of the same kind
	 * as returned by {@link org.springframework.xd.module.ModuleDefinitions#simple(String, org.springframework.xd.module.ModuleType, String)}
	 * but the 'location' must not be relied upon.
	 * @param name the name of the module
	 * @param type the type of the module
	 */
	public static ModuleDefinition dummy(String name, ModuleType type) {
		try {
			File location = File.createTempFile("dummy-module", type + name);
			Assert.isTrue(location.delete(), "could not delete temp file");
			Assert.isTrue(location.mkdirs(), "could not re-create file as a dir");
			location.deleteOnExit();
			return new SimpleModuleDefinition(name, type, "file:" + location.getAbsolutePath());
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
