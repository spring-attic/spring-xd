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

package org.springframework.xd.dirt.server.options;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.env.ConfigurableEnvironment;


/**
 * Logic to determine whether a command line option should override an existing system property or environment variable.
 * In general, command line options override existing environment. However command line options may contain default
 * values. If an option is not explicitly set by a command line argument the existing property value takes precedence.
 * 
 * @author David Turanski
 * @since 1.0
 */
class XDPropertyConfigurer {

	private final Properties xdProperties;

	private final ConfigurableEnvironment environment;

	private final AbstractOptions options;

	private static Log log = LogFactory.getLog(XDPropertyConfigurer.class);

	XDPropertyConfigurer(ConfigurableEnvironment environment, AbstractOptions options) {
		this.xdProperties = new Properties();
		this.environment = environment;
		this.options = options;
	}

	void handleCommandLineOption(String propertyKey, Object option, String optionValue) {
		if (!environment.containsProperty(propertyKey) || options.isExplicit(option)) {
			xdProperties.setProperty(propertyKey, optionValue == null ? option.toString() : optionValue);
			if (log.isDebugEnabled()) {
				log.debug("Setting property " + propertyKey + " from command line option. New value:"
						+ xdProperties.getProperty(propertyKey));
			}
		}
	}

	Properties getOverrideProperties() {
		return this.xdProperties;
	}
}
