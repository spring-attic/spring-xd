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

package org.springframework.xd.dirt.module;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;

/**
 * Provides common behavior for {@link ModuleRegistry} and knows how to handle custom classpath population for storages
 * that support it.
 * 
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Gary Russell
 */
public abstract class AbstractModuleRegistry implements ModuleRegistry, ApplicationContextAware {

	private ResourcePatternResolver resolver;

	@Override
	public ModuleDefinition lookup(String name, String type) {
		Resource resource = this.loadResource(name, type);
		URL[] classpath = maybeLocateClasspath(resource, name);
		ModuleDefinition module = new ModuleDefinition(name, type, resource, classpath);
		// TODO: add properties from a property registry
		return module;
	}

	/**
	 * Return an array of jar files locations or {@code null} if the module is a plain xml file.
	 */
	private URL[] maybeLocateClasspath(Resource resource, String name) {
		try {
			URL resourceLocation = resource.getURL();
			if (resourceLocation.toString().endsWith(name + "/config/" + name + ".xml")) {
				Resource jarsPattern = resource.createRelative("../lib/*.jar");
				Resource[] jarsResources = resolver.getResources(jarsPattern.getURI().toString());
				URL[] result = new URL[jarsResources.length];
				for (int i = 0; i < jarsResources.length; i++) {
					result[i] = jarsResources[i].getURL();
				}
				return result;
			}
			else {
				return null;
			}
		}
		catch (IOException ignored) {
			return null;
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		resolver = applicationContext;
	}

	@Override
	public List<ModuleDefinition> findDefinitions(String name) {
		ArrayList<ModuleDefinition> definitions = new ArrayList<ModuleDefinition>();
		for (ModuleType type : ModuleType.values()) {
			Resource resource = loadResource(name, type.getTypeName());
			if (resource != null) {
				ModuleDefinition moduleDef = new ModuleDefinition(name, type.getTypeName(), resource);
				definitions.add(moduleDef);
			}
		}
		return definitions;
	}

	protected abstract Resource loadResource(String name, String type);
}
