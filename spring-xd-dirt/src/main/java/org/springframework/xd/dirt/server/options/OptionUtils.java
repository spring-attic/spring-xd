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
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author David Turanski
 */
public class OptionUtils {

	/**
	 * Build XD Properties from options, partitioning by explicit and non-explicit command line arguments
	 * 
	 * @param options - the options
	 * @param explicit - true if explicit options are included, false otherwise
	 */
	static Properties optionsToProperties(AbstractOptions options, boolean explicit) {
		Properties xdProperties = new Properties();

		boolean explicitOption;

		explicitOption = options.isExplicit(options.getXdHomeDir());
		if ((explicitOption && explicit)
				|| (!explicitOption && !explicit)) {
			xdProperties.setProperty(XDPropertyKeys.XD_HOME, options.getXdHomeDir());
		}
		explicitOption = options.isExplicit(options.getTransport());
		if ((explicitOption && explicit)
				|| (!explicitOption && !explicit)) {
			xdProperties.setProperty(XDPropertyKeys.XD_TRANSPORT, options.getTransport().name());
		}
		explicitOption = options.isExplicit(options.getAnalytics());
		if ((explicitOption && explicit)
				|| (!explicitOption && !explicit)) {
			xdProperties.setProperty(XDPropertyKeys.XD_ANALYTICS, options.getAnalytics().name());
		}
		explicitOption = options.isExplicit(options.getHadoopDistro());
		if ((explicitOption && explicit)
				|| (!explicitOption && !explicit)) {
			xdProperties.setProperty(XDPropertyKeys.XD_HADOOP_DISTRO, options.getHadoopDistro().name());
		}
		explicitOption = options.isExplicit(options.isJmxEnabled());
		if ((explicitOption && explicit)
				|| (!explicitOption && !explicit)) {
			xdProperties.setProperty(XDPropertyKeys.XD_JMX_ENABLED, String.valueOf(options.isJmxEnabled()));
		}
		explicitOption = options.isExplicit(options.getJmxPort());
		if ((explicitOption && explicit)
				|| (!explicitOption && !explicit)) {
			xdProperties.setProperty(XDPropertyKeys.XD_JMX_PORT, String.valueOf(options.getJmxPort()));
		}
		explicitOption = options.isExplicit(options.getStore());
		if ((explicitOption && explicit)
				|| (!explicitOption && !explicit)) {
			xdProperties.setProperty(XDPropertyKeys.XD_STORE, options.getStore().name());
		}
		if (options instanceof AdminOptions) {
			AdminOptions adminOptions = (AdminOptions) options;
			explicitOption = adminOptions.isExplicit(adminOptions.getHttpPort());
			if ((explicitOption && explicit)
					|| (!explicitOption && !explicit)) {
				xdProperties.setProperty(XDPropertyKeys.XD_HTTP_PORT, String.valueOf(adminOptions.getHttpPort()));
			}
		}
		return xdProperties;
	}

	/**
	 * Configure the XD runtime enviroment using parsed command line options
	 * 
	 * @param options Command line options
	 * @param environment the application Context environment
	 */
	public static void configureRuntime(AbstractOptions options, ConfigurableEnvironment environment) {

		Properties overrideProperties = optionsToProperties(options, true);
		if (!CollectionUtils.isEmpty(overrideProperties)) {
			environment.getPropertySources().addFirst(new PropertiesPropertySource(
					"xdProperties", overrideProperties));
		}
		Properties defaultProperties = optionsToProperties(options, false);
		if (!CollectionUtils.isEmpty(defaultProperties)) {
			environment.getPropertySources().addLast(new PropertiesPropertySource(
					"xdDefaults", defaultProperties));
		}

		Assert.isTrue(environment.containsProperty(XDPropertyKeys.XD_HOME), "XD_HOME is not set");
		Assert.isTrue(environment.containsProperty(XDPropertyKeys.XD_TRANSPORT), "XD_TRANSPORT is not set");
		Assert.isTrue(environment.containsProperty(XDPropertyKeys.XD_STORE), "XD_STORE is not set");
		if (environment.getProperty(XDPropertyKeys.XD_JMX_ENABLED).equals("true")) {
			environment.addActiveProfile("xd.jmx.enabled");
		}
	}
}
