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
	 * @param options
	 * @param environment
	 */
	public static void setJmxProperties(AbstractOptions options, ConfigurableEnvironment environment) {
		Properties jmxProperties = new Properties();
		jmxProperties.put(XDPropertyKeys.XD_JMX_PORT, options.getJmxPort());
		environment.getPropertySources().addFirst(new PropertiesPropertySource("jmxProperties", jmxProperties));
	}

	/**
	 * Configure the XD runtime enviroment using parsed command line options
	 * 
	 * @param options Command line options
	 * @param environment the application Context environment
	 */
	public static void configureRuntime(AbstractOptions options, ConfigurableEnvironment environment) {
		setSystemProperty(XDPropertyKeys.XD_HOME, options.getXDHomeDir(), false);
		setSystemProperty(XDPropertyKeys.XD_TRANSPORT, options.getTransport().name(), false);
		setSystemProperty(XDPropertyKeys.XD_ANALYTICS, options.getAnalytics().name(), false);
		setSystemProperty(XDPropertyKeys.XD_JMX_ENABLED, String.valueOf(options.isJmxEnabled()), false);
		if (options instanceof AdminOptions) {
			setSystemProperty(XDPropertyKeys.XD_STORE, ((AdminOptions) options).getStore().name(), false);
		}
		if (environment != null) {
			if (options.isJmxEnabled()) {
				environment.addActiveProfile("xd.jmx.enabled");
				OptionUtils.setJmxProperties(options, environment);
			}
		}
	}

	public static String setSystemProperty(String key, String value, boolean override) {
		if (System.getProperty(key) == null || override) {
			System.setProperty(key, value);
		}
		return System.getProperty(key);
	}
}
