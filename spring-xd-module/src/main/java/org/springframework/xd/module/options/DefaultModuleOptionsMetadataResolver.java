/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.module.options;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.support.ParentLastURLClassLoader;

/**
 * The default implementation of {@link ModuleOptionsMetadataResolver} that deals with simple modules and reads the
 * companion {@code .properties} file sitting next to the module definition.
 * 
 * <p>
 * The following strategies will be applied in turn:
 * <ul>
 * <li>look for a file named {@code <modulename>.properties} next to the module xml definition file</li>
 * <li>if that file exists
 * <ul>
 * <li>look for an {@value ModuleOptionsMetadataResolver#OPTIONS_CLASS} property. If found, use a
 * {@link PojoModuleOptionsMetadata} backed by that POJO classname</li>
 * <li>use a {@link SimpleModuleOptionsMetadata} backed by keys of the form {@code options.<name>.description}.
 * Additionaly, one can provide {@code options.<name>.default} and {@code options.<name>.type} properties.</li>
 * </ul>
 * <li>return an instance of {@link DefaultModuleOptionsMetadata}.
 * <ul>
 * 
 * 
 * @author Eric Bottard
 */
public class DefaultModuleOptionsMetadataResolver implements ModuleOptionsMetadataResolver, EnvironmentAware,
		ResourceLoaderAware {


	private static final Pattern DESCRIPTION_KEY_PATTERN = Pattern.compile("^options\\.([a-zA-Z\\-_0-9]+)\\.description$");

	/**
	 * Name of the property containing a POJO fully qualified classname, which will be used to create a
	 * {@link PojoModuleOptionsMetadata}.
	 */
	private static final String OPTIONS_CLASS = "options_class";

	private static final Map<String, Class<?>> SHORT_CLASSNAMES = new HashMap<String, Class<?>>();

	static {
		SHORT_CLASSNAMES.put("String", String.class);
		SHORT_CLASSNAMES.put("boolean", boolean.class);
		SHORT_CLASSNAMES.put("Boolean", Boolean.class);
		SHORT_CLASSNAMES.put("int", int.class);
		SHORT_CLASSNAMES.put("Integer", Integer.class);
		SHORT_CLASSNAMES.put("long", long.class);
		SHORT_CLASSNAMES.put("Long", Long.class);
		SHORT_CLASSNAMES.put("float", float.class);
		SHORT_CLASSNAMES.put("Float", Float.class);
		SHORT_CLASSNAMES.put("double", double.class);
		SHORT_CLASSNAMES.put("Double", Double.class);
	}

	/**
	 * Used to resolve the possible placeholders in the location of a defaults file for
	 * {@link PojoModuleOptionsMetadata} (as stated in a @{@link PropertySource} annotation).
	 */
	private Environment environment;

	/**
	 * Used to actually load the {@code .properties} file(s) that may be referenced in @{@link PropertySource}
	 * annotations in {@link PojoModuleOptionsMetadata}.
	 */
	private ResourceLoader resourceLoader;

	private ModuleOptionsMetadata makeSimpleModuleOptions(Properties props) {
		SimpleModuleOptionsMetadata result = new SimpleModuleOptionsMetadata();
		for (Object key : props.keySet()) {
			if (key instanceof String) {
				String propName = (String) key;
				Matcher matcher = DESCRIPTION_KEY_PATTERN.matcher(propName);
				if (!matcher.matches()) {
					continue;
				}
				String optionName = matcher.group(1);
				String description = props.getProperty(propName);
				Object defaultValue = props.getProperty(String.format("options.%s.default", optionName));
				String type = props.getProperty(String.format("options.%s.type", optionName));
				Class<?> clazz = null;
				if (type != null) {
					if (SHORT_CLASSNAMES.containsKey(type)) {
						clazz = SHORT_CLASSNAMES.get(type);
					}
					else {
						try {
							clazz = Class.forName(type);
						}
						catch (ClassNotFoundException e) {
							throw new IllegalStateException("Can't find class used for type of option '"
									+ optionName
									+ "': " + type);
						}
					}
				}
				ModuleOption moduleOption = new ModuleOption(optionName, description).withDefaultValue(
						defaultValue).withType(clazz);
				result.add(moduleOption);
			}
		}
		return result;
	}

	@Override
	public ModuleOptionsMetadata resolve(ModuleDefinition definition) {
		try {
			Resource propertiesResource = definition.getResource().createRelative(
					definition.getName() + ".properties");
			if (!propertiesResource.exists()) {
				return new DefaultModuleOptionsMetadata();
			}
			else {
				Properties props = new Properties();
				props.load(propertiesResource.getInputStream());
				String pojoClass = props.getProperty(OPTIONS_CLASS);
				if (pojoClass != null) {
					try {
						ClassLoader classLoaderToUse = definition.getClasspath() != null
								? new ParentLastURLClassLoader(definition.getClasspath(),
										ModuleOptionsMetadataResolver.class.getClassLoader())
								: ModuleOptionsMetadataResolver.class.getClassLoader();
						Class<?> clazz = Class.forName(pojoClass, true, classLoaderToUse);
						return new PojoModuleOptionsMetadata(clazz, resourceLoader, environment);
					}
					catch (ClassNotFoundException e) {
						throw new IllegalStateException("Unable to load class used by ModuleOptionsMetadata: "
								+ pojoClass, e);
					}
				}
				return makeSimpleModuleOptions(props);
			}
		}
		catch (IOException e) {
			return new DefaultModuleOptionsMetadata();
		}

	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}


}
