/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.module.core;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;


/**
 * Tests for {@link ModuleEnvironment}.
 * 
 * @author Eric Bottard
 */
public class ModuleEnvironmentTests {

	private static final Random random = new Random();

	@Test
	public void testLegitOptionResolution() {
		ConfigurableEnvironment parentEnv = new AbstractEnvironment() {

			@Override
			protected void customizePropertySources(MutablePropertySources propertySources) {
				propertySources.addFirst(singleton("user", "eric"));
			}
		};
		EnumerablePropertySource<?> modulePS = singleton("host", "localhost");
		ModuleEnvironment moduleEnvironment = new ModuleEnvironment(modulePS, parentEnv);
		assertThat(moduleEnvironment.getProperty("user"), equalTo("eric"));
		assertThat(moduleEnvironment.getProperty("host"), equalTo("localhost"));

	}

	@Test
	public void testIllegalOptionResolution() {
		ConfigurableEnvironment parentEnv = new AbstractEnvironment() {

			@Override
			protected void customizePropertySources(MutablePropertySources propertySources) {
				propertySources.addFirst(singleton("user", "eric"));
			}
		};
		EnumerablePropertySource<?> modulePS = singleton("user", null);
		ModuleEnvironment moduleEnvironment = new ModuleEnvironment(modulePS, parentEnv);
		assertThat(moduleEnvironment.getProperty("user"), nullValue());

	}

	private EnumerablePropertySource<?> singleton(String key, Object value) {
		Map<String, Object> map = Collections.singletonMap(key, value);
		return new MapPropertySource("ps-" + random.nextInt(), map);
	}


}
