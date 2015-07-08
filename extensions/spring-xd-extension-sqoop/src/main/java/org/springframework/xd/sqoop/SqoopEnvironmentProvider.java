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

package org.springframework.xd.sqoop;

import org.springframework.batch.step.tasklet.x.EnvironmentProvider;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Class that sets environment variables for Sqoop
 */
public class SqoopEnvironmentProvider implements EnvironmentProvider {

	private static final String XD_CONFIG_HOME = "xd.config.home";

	ConfigurableEnvironment environment;

	String libjars;

	public SqoopEnvironmentProvider(ConfigurableEnvironment environment, String libjars) {
		this.environment = environment;
		this.libjars = libjars;
	}

	@Override
	public void setEnvironment(Map<String, String> env) {
		String javaHome = System.getenv("JAVA_HOME");
		if (StringUtils.hasText(javaHome)) {
			env.put("JAVA_HOME", javaHome);
		}
		env.put("SQOOP_CONF_DIR", environment.getProperty(XD_CONFIG_HOME));
		if (StringUtils.hasText(libjars)) {
			env.put("XD_MODULE_LIBJARS", libjars);
		}
	}

}
