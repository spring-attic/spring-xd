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

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author David Turanski
 */
public class OptionUtils {

	/**
	 * Configure the XD runtime enviroment using parsed command line options
	 * 
	 * @param options Command line options
	 * @param environment the application Context environment
	 */
	public static void configureRuntime(AbstractOptions options, ConfigurableEnvironment environment) {
		XDPropertyConfigurer propertyConfigurer = new XDPropertyConfigurer(environment, options);

		propertyConfigurer.handleCommandLineOption(XDPropertyKeys.XD_HOME,
				options.getXDHomeDir(), null);
		propertyConfigurer.handleCommandLineOption(XDPropertyKeys.XD_TRANSPORT,
				options.getTransport(), options.getTransport().name());
		propertyConfigurer.handleCommandLineOption(XDPropertyKeys.XD_ANALYTICS,
				options.getAnalytics(), options.getAnalytics().name());
		propertyConfigurer.handleCommandLineOption(XDPropertyKeys.XD_TRANSPORT,
				options.getTransport(), options.getTransport().name());
		propertyConfigurer.handleCommandLineOption(XDPropertyKeys.XD_JMX_ENABLED,
				options.isJmxEnabled(),
				String.valueOf(options.isJmxEnabled()));

		if (options instanceof AdminOptions) {
			propertyConfigurer.handleCommandLineOption(XDPropertyKeys.XD_STORE,
					((AdminOptions) options).getStore(),
					((AdminOptions) options).getStore().name());
			propertyConfigurer.handleCommandLineOption(XDPropertyKeys.XD_HTTP_PORT,
					((AdminOptions) options).getHttpPort(), String.valueOf(((AdminOptions) options).getHttpPort()));
		}

		propertyConfigurer.handleCommandLineOption(XDPropertyKeys.XD_JMX_ENABLED,
				options.isJmxEnabled(), String.valueOf(options.isJmxEnabled()));
		if (options.isJmxEnabled()) {
			propertyConfigurer.handleCommandLineOption(XDPropertyKeys.XD_JMX_PORT,
					options.getJmxPort(), String.valueOf(options.getJmxPort()));
		}

		if (!CollectionUtils.isEmpty(propertyConfigurer.getOverrideProperties())) {
			environment.getPropertySources().addFirst(new PropertiesPropertySource(
					"xdProperties", propertyConfigurer.getOverrideProperties()));
		}
		Assert.isTrue(environment.containsProperty(XDPropertyKeys.XD_HOME), "XD_HOME is not set");
		Assert.isTrue(environment.containsProperty(XDPropertyKeys.XD_TRANSPORT), "XD_TRANSPORT is not set");
	}
}
