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

package org.springframework.xd.dirt.rest.security;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;

import com.google.common.collect.ImmutableMap;


/**
 * Custom initializer for {@link SecurityControllerIntegrationTests}. Customizes
 * the {@link Environment} and sets {@code security.basic.enabled=false}.
 *
 * @author Gunnar Hillert
 */
public class SecurityControllerIntegrationTestsInitializer implements
ApplicationContextInitializer<ConfigurableApplicationContext> {

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		final MapPropertySource propertySource = new MapPropertySource("test", ImmutableMap.<String, Object> of(
				"security.basic.enabled", String.valueOf(false)));
		applicationContext.getEnvironment().getPropertySources().addFirst(propertySource);
	}
}
