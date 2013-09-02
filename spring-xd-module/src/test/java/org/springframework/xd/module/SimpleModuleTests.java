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

package org.springframework.xd.module;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;

import org.springframework.core.io.ClassPathResource;


/**
 * Tests behavior of {@link SimpleModule}.
 * 
 * @author Eric Bottard
 */
public class SimpleModuleTests {

	@Test
	public void testProfilesAreActivated() {

		// default
		ModuleDefinition definition = new ModuleDefinition("test", "source", new ClassPathResource(
				"/test-modules/source/test.xml"));
		SimpleModule module = new SimpleModule(definition, new DeploymentMetadata("foo", 0));

		String bean = module.getComponent("foo", String.class);
		assertEquals("default", bean);

		// Activate option with some value
		definition = new ModuleDefinition("test", "source", new ClassPathResource("/test-modules/source/test.xml"));
		Properties properties = new Properties();
		properties.setProperty("myoption", "somevalue");
		definition.setProperties(properties);
		module = new SimpleModule(definition, new DeploymentMetadata("foo", 0));

		bean = module.getComponent("foo", String.class);
		assertEquals("myoption-is-set", bean);


		// Activate option with specific value
		definition = new ModuleDefinition("test", "source", new ClassPathResource("/test-modules/source/test.xml"));
		properties = new Properties();
		properties.setProperty("myoption", "myvalue");
		definition.setProperties(properties);
		module = new SimpleModule(definition, new DeploymentMetadata("foo", 0));

		bean = module.getComponent("foo", String.class);
		assertEquals("myoption-is-set-to-myvalue", bean);


	}
}
