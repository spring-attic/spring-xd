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

package org.springframework.xd.extension.script;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author David Turanski
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = {PropertiesInjector.class})
public class ScriptModulePropertiesFactoryBeanTest {
	@Autowired @Qualifier("props") Properties properties;

	@Test
	public void test() {
		assertEquals("BAZ", properties.getProperty("baz"));
		assertEquals("CAR", properties.getProperty("car"));
	}
}

class PropertiesInjector implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		Properties variables = new Properties();
		variables.put("variables", " baz  = BAZ,  car = CAR");
		applicationContext.getEnvironment().getPropertySources()
				.addLast(new PropertiesPropertySource("variables", variables));
	}
}
