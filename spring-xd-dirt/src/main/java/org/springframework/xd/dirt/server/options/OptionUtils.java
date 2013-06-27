/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.xd.dirt.server.options;

import java.util.Properties;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

/**
 * @author David Turanski
 *
 */
public class OptionUtils {
	/**
	 * @parame options
	 * @param environment
	 */
	public static void setJmxProperties(AbstractOptions options, ConfigurableEnvironment environment) {
		Properties jmxProperties = new Properties();
		jmxProperties.put(AbstractOptions.XD_JMX_PORT_KEY, options.getJmxPort());	
		environment.getPropertySources().addFirst(new PropertiesPropertySource("jmxProperties",jmxProperties));
	}

}
