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

package org.springframework.xd.module.options;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.xd.module.ModuleDefinition;


/**
 * A decorator around another {@link ModuleOptionsMetadataResolver} that will provide default values for module options
 * using the environment.
 * 
 * <p>
 * Each module gets its own Environment, populated with values in the following order:
 * <ul>
 * <li>System properties and environment variables</li>
 * <li>Values in a properties file found at {@code $XD_MODULE_CONFIG_LOCATION/<type>/<module>/<module>.properties}.
 * Mappings in this file shall not use the fully qualified form, but rather the simple form
 * {@code <optionname>=<optionvalue>}</li>
 * <li>Values in a yml file found at {@code $XD_MODULE_CONFIG_LOCATION/$XD_MODULE_CONFIG_NAME}. Mappings in this file
 * use the fully qualified form (see below)</li>
 * </ul>
 * <p>
 * For each option {@code <optionname>} of a module (of type {@code <type>} and name {@code <modulename>}), this
 * resolver will try to read a default from {@code <type>.<modulename>.<optionname>}.
 * 
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
public class EnvironmentAwareModuleOptionsMetadataResolver implements ModuleOptionsMetadataResolver,
		ResourceLoaderAware, EnvironmentAware
{

	/**
	 * Name of the configuration key that holds the location root for module configuration.
	 */
	private static final String XD_MODULE_CONFIG_LOCATION = "xd.module.config.location";

	/**
	 * Name of the configuration key that holds the base file name for global module configuration.
	 */
	private static final String XD_MODULE_CONFIG_NAME = "xd.module.config.name";

	/**
	 * The default value for key {@link #XD_MODULE_CONFIG_NAME}.
	 */
	private static final String DEFAULT_XD_MODULE_CONFIG_NAME = "modules";

	/**
	 * The name of the property source that Spring Boot will create.
	 */
	private static final String APPLICATION_CONFIGURATION_PROPERTIES = "applicationConfigurationProperties";

	private ModuleOptionsMetadataResolver delegate;

	private String xdModuleConfigLocation;


	@Value("${" + XD_MODULE_CONFIG_LOCATION + ":${xd.config.home}/modules/}")
	public void setXdModuleConfigLocation(String xdModuleConfigLocation) {
		// TODO: Need to fix by removing this specific requirement as this poses explicit requirement even in windows
		Assert.isTrue(xdModuleConfigLocation.endsWith("/"),
				String.format("'%s' must end with a '/'", XD_MODULE_CONFIG_LOCATION));
		this.xdModuleConfigLocation = xdModuleConfigLocation;
	}

	/**
	 * An environment that reflects values in the {@code modules.yml} file.
	 */
	private ConfigurableEnvironment rootEnvironment;

	private String configName = DEFAULT_XD_MODULE_CONFIG_NAME;

	private ResourceLoader resourceLoader;

	/**
	 * The parent environment this bean lives in. Used to know which profiles are active at the server level.
	 */
	private ConfigurableEnvironment parentEnvironment;

	@Value("${" + XD_MODULE_CONFIG_NAME + ":" + DEFAULT_XD_MODULE_CONFIG_NAME + "}")
	public void setConfigName(String configName) {
		boolean valid = StringUtils.hasText(configName) && !configName.endsWith(".properties")
				&& !configName.endsWith(".yml");
		Assert.isTrue(valid,
				String.format("'%s' should not be blank, nor end up with a file extension", XD_MODULE_CONFIG_NAME));
		this.configName = configName;
	}

	public void setDelegate(ModuleOptionsMetadataResolver delegate) {
		this.delegate = delegate;
	}

	@Override
	public ModuleOptionsMetadata resolve(ModuleDefinition moduleDefinition) {
		ModuleOptionsMetadata wrapped = delegate.resolve(moduleDefinition);
		if (wrapped == null) {
			return null;
		}

		return new ModuleOptionsMetadataWithDefaults(wrapped, moduleDefinition);
	}

	private class ModuleOptionsMetadataWithDefaults implements ModuleOptionsMetadata {

		private final ModuleOptionsMetadata wrapped;

		private final ModuleDefinition moduleDefinition;

		public ModuleOptionsMetadataWithDefaults(ModuleOptionsMetadata wrapped, ModuleDefinition moduleDefinition) {
			this.wrapped = wrapped;
			this.moduleDefinition = moduleDefinition;
			Environment moduleEnvironment = lookupEnvironment(moduleDefinition);
			for (ModuleOption original : wrapped) {
				Object newDefault = computeDefault(original, moduleEnvironment);
				if (newDefault != null) {
					// This changes the value by side effect
					original.withDefaultValue(newDefault);
				}
			}
		}

		@Override
		public Iterator<ModuleOption> iterator() {
			return wrapped.iterator();
		}

		@Override
		public ModuleOptions interpolate(Map<String, String> raw) throws BindException {
			Map<String, String> rawPlusDefaults = new HashMap<String, String>(raw);
			for (ModuleOption option : wrapped) {
				if (raw.containsKey(option.getName()) || option.getDefaultValue() == null) {
					continue;
				}
				rawPlusDefaults.put(option.getName(), "" + option.getDefaultValue());
			}
			return wrapped.interpolate(rawPlusDefaults);
		}

		private Object computeDefault(ModuleOption option, Environment moduleEnvironment) {
			String fqKey = fullyQualifiedKey(moduleDefinition, option.getName());
			return moduleEnvironment.getProperty(fqKey);
		}


	}

	private Environment lookupEnvironment(ModuleDefinition moduleDefinition) {
		// load rootEnvironment at runtime than during the startup
		rootEnvironment = loadPropertySources(xdModuleConfigLocation, configName);
		String propertySourceName = String.format("%s:%s",
				moduleDefinition.getType(), moduleDefinition.getName());
		// Load short name values into a throwaway env
		String path = String.format("%s%s/%s/", xdModuleConfigLocation, moduleDefinition.getType(),
				moduleDefinition.getName());
		ConfigurableEnvironment throwAwayEnvironment = loadPropertySources(path, moduleDefinition.getName());
		EnumerablePropertySource<?> nakedPS = (EnumerablePropertySource<?>) throwAwayEnvironment.getPropertySources().get(
				APPLICATION_CONFIGURATION_PROPERTIES);
		// Now transform them to their fully qualified form
		Map<String, Object> values = new HashMap<String, Object>();
		for (String name : nakedPS.getPropertyNames()) {
			values.put(fullyQualifiedKey(moduleDefinition, name), nakedPS.getProperty(name));
		}
		EnumerablePropertySource<?> modulePS = new MapPropertySource(propertySourceName, values);
		ConfigurableEnvironment moduleEnvironment = new StandardEnvironment();
		// Append the rootEnvironment
		moduleEnvironment.merge(rootEnvironment);
		// The global environment has been loaded by boot too and
		// its PS of interest was also named "applicationConfigurationProperties"
		moduleEnvironment.getPropertySources().addBefore(APPLICATION_CONFIGURATION_PROPERTIES, modulePS);
		return moduleEnvironment;
	}

	/**
	 * Craft the "fully qualified" key for a given module option name.
	 */
	private String fullyQualifiedKey(ModuleDefinition moduleDefinition, String optionName) {
		return String.format("%s.%s.%s", moduleDefinition.getType(), moduleDefinition.getName(), optionName);
	}

	/**
	 * Construct a new environment and use Spring Boot to populate its property sources using
	 * {@link ConfigFileApplicationListener}.
	 */
	private ConfigurableEnvironment loadPropertySources(final String searchLocation, final String baseName) {
		final ConfigurableEnvironment environment = new StandardEnvironment();
		environment.merge(parentEnvironment);
		new ConfigFileApplicationListener() {

			public void apply() {
				setSearchLocations(searchLocation);
				// We'd like to do 'setSearchNames(baseName)', but the environment property
				// has strong precedence and is already set for XD_CONFIG_NAME.
				Map<String, Object> singletonMap = Collections.singletonMap("spring.config.name",
						(Object) baseName);
				environment.getPropertySources().addFirst(
						new MapPropertySource("searchNamesOverride", singletonMap));
				addPropertySources(environment, resourceLoader);
			}
		}.apply();
		return environment;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.parentEnvironment = (ConfigurableEnvironment) environment;
	}

}
