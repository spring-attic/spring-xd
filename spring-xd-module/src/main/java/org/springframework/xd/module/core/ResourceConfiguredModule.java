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

package org.springframework.xd.module.core;

import org.springframework.core.io.Resource;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.SimpleModuleDefinition;
import org.springframework.xd.module.options.ModuleOptions;
import org.springframework.xd.module.support.ModuleUtils;

/**
 * A {@link org.springframework.xd.module.core.SimpleModule} configured using a bean definition resource (XML or
 * Groovy)
 *
 * @author David Turanski
 * @author Eric Bottard
 */
public class ResourceConfiguredModule extends SimpleModule {

	public ResourceConfiguredModule(ModuleDescriptor descriptor, ModuleDeploymentProperties deploymentProperties) {
		super(descriptor, deploymentProperties);
	}

	// Do not remove; Invoked via reflection
	@SuppressWarnings("unused")
	public ResourceConfiguredModule(ModuleDescriptor descriptor, ModuleDeploymentProperties deploymentProperties,
			ClassLoader classLoader, ModuleOptions moduleOptions) {
		super(descriptor, deploymentProperties, classLoader, moduleOptions);
	}

	/**
	 * Return the resource that can be used to configure a module, or null if no such resource exists.
	 *
	 * @throws java.lang.IllegalStateException if both a .xml and .groovy file are present
	 */
	public static Resource resourceBasedConfigurationFile(SimpleModuleDefinition moduleDefinition) {
		Resource xml = ModuleUtils.locateModuleResource(moduleDefinition, ".xml");
		Resource groovy = ModuleUtils.locateModuleResource(moduleDefinition, ".groovy");
		boolean xmlExists = xml != null;
		boolean groovyExists = groovy != null;
		if (xmlExists && groovyExists) {
			throw new IllegalStateException(String.format("Found both resources '%s' and '%s' for module %s", xml,
					groovy, moduleDefinition));
		}
		else if (xmlExists) {
			return xml;
		}
		else if (groovyExists) {
			return groovy;
		}
		else {
			return null;
		}

	}

	@Override
	protected void configureModuleApplicationContext(SimpleModuleDefinition moduleDefinition) {
		Resource source = resourceBasedConfigurationFile(moduleDefinition);
		if (source != null) {
			addSource(source);
		}
	}

}
