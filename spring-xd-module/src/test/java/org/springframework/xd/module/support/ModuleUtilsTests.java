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

package org.springframework.xd.module.support;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import org.springframework.xd.module.ModuleDefinitions;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.SimpleModuleDefinition;
import org.springframework.xd.module.options.ModuleOption;
import org.springframework.xd.module.options.ModuleOptions;
import org.springframework.xd.module.options.ModuleUtils;
import org.springframework.xd.module.options.SimpleModuleOptionsMetadata;

public class ModuleUtilsTests {

	@Test
	public void testModuleOptionInterpolation() throws Exception {
		SimpleModuleDefinition definition = ModuleDefinitions.simple("foobar", ModuleType.source, "file:src/test/resources/ModuleUtilsTests/dynamic_classpath/source/foobar/");
		SimpleModuleOptionsMetadata metadata = new SimpleModuleOptionsMetadata();
		metadata.add(new ModuleOption("version", "the version to use").withDefaultValue("dynamic1"));
		ModuleOptions options = metadata.interpolate(Collections.<String, String>emptyMap());
		ClassLoader classLoader = ModuleUtils.createModuleRuntimeClassLoader(definition, options, ModuleUtilsTests.class.getClassLoader());

		String value = new BufferedReader(new InputStreamReader(classLoader.getResourceAsStream("some_resource"))).readLine();

		assertThat(value, CoreMatchers.equalTo("value1"));

		options = metadata.interpolate(Collections.singletonMap("version", "dynamic2"));
		classLoader = ModuleUtils.createModuleRuntimeClassLoader(definition, options, ModuleUtilsTests.class.getClassLoader());
		value = new BufferedReader(new InputStreamReader(classLoader.getResourceAsStream("some_resource"))).readLine();

		assertThat(value, CoreMatchers.equalTo("value2"));
	}

	@Test
	public void testUnresolvablePlaceholdersDontCrash() throws Exception {
		SimpleModuleDefinition definition = ModuleDefinitions.simple("foobar", ModuleType.source, "file:src/test/resources/ModuleUtilsTests/dynamic_classpath/source/foobar/");
		SimpleModuleOptionsMetadata metadata = new SimpleModuleOptionsMetadata();
		ModuleOptions options = metadata.interpolate(Collections.<String, String>emptyMap());
		ClassLoader classLoader = ModuleUtils.createModuleRuntimeClassLoader(definition, options, ModuleUtilsTests.class.getClassLoader());
		String value = new BufferedReader(new InputStreamReader(classLoader.getResourceAsStream("a_file"))).readLine();

		assertThat(value, CoreMatchers.equalTo("with some value"));
	}

	@Test
	public void testClassLoaderInsulationWhenLoadingResource() throws Exception {
		SimpleModuleDefinition definition = ModuleDefinitions.simple("foobar", ModuleType.source, "file:src/test/resources/ModuleUtilsTests/dynamic_classpath/source/foobar/");
		// Would fail if invisible.properties was visible
		ModuleUtils.modulePropertiesFile(definition);
	}

	@Test(expected = IllegalStateException.class)
	public void testFailureWhenMultipleResourcesFound() throws Exception {
		SimpleModuleDefinition definition = ModuleDefinitions.simple("foobar", ModuleType.source, "file:src/test/resources/ModuleUtilsTests/multiple_resources/");
		ModuleUtils.modulePropertiesFile(definition);
	}
}
