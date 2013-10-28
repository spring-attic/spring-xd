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

package org.springframework.xd.module.options;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.Resource;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ParentLastURLClassLoader;


/**
 * Factory class for deriving {@link ModuleOptions} for a given {@link ModuleDefinition}.
 * 
 * The following strategies will be applied in turn:
 * <ul>
 * <li>look for a file named {@code <modulename>.properties} next to the module xml definition file</li>
 * <li>if that file exists
 * <ul>
 * <li>look for an {@value ModuleOptionsFactory#OPTIONS_CLASS} property. If found, use a {@link PojoModuleOptions}
 * backed by that POJO classname</li>
 * <li>use a {@link SimpleModuleOptions} backed by keys of the form {@code options.<name>.description}. Additionaly, one
 * can provide {@code options.<name>.default} and {@code options.<name>.type} properties.</li>
 * </ul>
 * <li>return an {@link AbsentModuleOptions}.
 * <ul>
 * 
 * @author Eric Bottard
 */
public class ModuleOptionsFactory {

	private static final Pattern DESCRIPTION_KEY_PATTERN = Pattern.compile("^options\\.([a-zA-Z\\-_0-9]+)\\.description$");

	/**
	 * Name of the property containing a POJO fully qualified classname, which will be used to create a
	 * {@link PojoModuleOptions}.
	 */
	private static final String OPTIONS_CLASS = "options_class";

	public static ModuleOptions create(ModuleDefinition definition) {
		try {
			Resource propertiesResource = definition.getResource().createRelative(definition.getName() + ".properties");
			if (!propertiesResource.exists()) {
				return AbsentModuleOptions.INSTANCE;
			}
			else {
				Properties props = new Properties();
				props.load(propertiesResource.getInputStream());
				String pojoClass = props.getProperty(OPTIONS_CLASS);
				if (pojoClass != null) {
					try {
						ClassLoader classLoaderToUse = definition.getClasspath() != null
								? new ParentLastURLClassLoader(definition.getClasspath(),
										ModuleOptionsFactory.class.getClassLoader())
								: ModuleOptionsFactory.class.getClassLoader();
						Class<?> clazz = Class.forName(pojoClass, true, classLoaderToUse);
						return new PojoModuleOptions(clazz);
					}
					catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				}
				return makeSimpleModuleOptions(props);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			return AbsentModuleOptions.INSTANCE;
		}

	}

	private static ModuleOptions makeSimpleModuleOptions(Properties props) {
		SimpleModuleOptions result = new SimpleModuleOptions();
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
					try {
						clazz = Class.forName(type);
					}
					catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
				ModuleOption moduleOption = new ModuleOption(optionName, description).withDefaultValue(
						defaultValue).withType(clazz);
				result.add(moduleOption);
			}
		}
		return result;
	}
}
