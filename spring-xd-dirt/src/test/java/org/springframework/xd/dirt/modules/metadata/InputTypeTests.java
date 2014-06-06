/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.modules.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.module.ResourceModuleRegistry;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.options.DefaultModuleOptionsMetadataResolver;
import org.springframework.xd.module.options.ModuleOption;
import org.springframework.xd.module.options.ModuleOptionsMetadata;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;
import org.springframework.xd.tuple.DefaultTuple;


/**
 * Tests about the special {@code inputType} option.
 * 
 * @author David Turanski
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = InputTypeTests.Config.class)
public class InputTypeTests {

	@Autowired
	private ModuleRegistry testModuleRegistry;

	@Autowired
	private ModuleOptionsMetadataResolver moduleOptionsMetadataResolver;

	@Test
	public void testDefaultInputType() {
		testInputTypeConfigured(
				testModuleRegistry.findDefinition("testtupleprocessor_pojo_config", ModuleType.processor),
				DefaultTuple.class.getName());

		testInputTypeConfigured(testModuleRegistry.findDefinition("testtupleprocessor", ModuleType.processor),
				"application/x-xd-tuple");
	}

	private void testInputTypeConfigured(ModuleDefinition md, Object expectedValue) {
		assertNotNull(md);
		ModuleOptionsMetadata moduleOptionsMetadata = moduleOptionsMetadataResolver.resolve(md);
		boolean hasInputType = false;
		for (ModuleOption mo : moduleOptionsMetadata) {
			if (mo.getName().equals("inputType")) {
				assertEquals(expectedValue, mo.getDefaultValue());
				hasInputType = true;
			}
		}
		assertTrue(hasInputType);
	}


	@Configuration
	public static class Config {

		@Autowired
		public void setEnvironment(Environment environment) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("xd.config.home", "file:../config");
			((ConfigurableEnvironment) environment).getPropertySources().addFirst(new MapPropertySource("foo", map));
		}

		@Bean
		public ModuleRegistry testModuleRegistry() {
			return new ResourceModuleRegistry("classpath:testmodules");
		}


		@Bean
		public ModuleOptionsMetadataResolver moduleOptionsMetadataResolver() {
			return new DefaultModuleOptionsMetadataResolver();
		}

	}
}
