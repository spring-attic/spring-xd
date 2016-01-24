/*
 * Copyright 2013-2014 the original author or authors.
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

import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;
import static org.springframework.core.env.StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;


/**
 * A dedicated {@link Environment} for a module, which restricts the values exposed to the
 * {@link PropertySourcesPlaceholderConfigurer} living in the module context.
 *
 * <p>
 * In particular, this prevents scenarios where <i>e.g.</i> the module would have ${username} in its definition, where
 * <i>username</i> is a valid module option name for which there is no current value (PS returns {@code null} but there
 * exists say, an environment variable of the same name. In such a case, the module would pick up that value by mistake.
 * </p>
 *
 * See https://jira.spring.io/browse/XD-1459.
 *
 * @author Eric Bottard
 * @author David Turanski
 */
public class ModuleEnvironment extends AbstractEnvironment {

	private ConfigurableEnvironment parent;

	public ModuleEnvironment(EnumerablePropertySource<?> moduleOptionsPropertySource, ConfigurableEnvironment parent) {
		this.parent = parent;
		if (parent != null) {
			merge(parent); // This will copy profiles (and property sources)
		}
		clearPropertySources(); // We don't want the property sources though

		getPropertySources().addFirst(wrap(moduleOptionsPropertySource));
	}

	/**
	 * Wrap the module options property source so that it can be the only one living in the environment. This will
	 * delegate to the parent environment ONLY IF the key is not a known module option name.
	 */
	private EnumerablePropertySource<?> wrap(final EnumerablePropertySource<?>
			moduleOptionsPropertySource) {
		return new EnumerablePropertySource<Object>(moduleOptionsPropertySource.getName
				(),
				moduleOptionsPropertySource) {

			@Override public String[] getPropertyNames() {
				return moduleOptionsPropertySource.getPropertyNames();
			}

			@Override
			public Object getProperty(String name) {
				// Resolve thru the module first
				Object result = moduleOptionsPropertySource.getProperty(name);
				if (result != null) {
					return result;
				}
				// If module could not resolve, but it is *known* to be
				// a valid module option name, do NOT delegate to the env
				// as it could end up in false positives
				if (moduleOptionsPropertySource.containsProperty(name)) {
					return null;
				}
				else {
					return parent == null ? null : parent.getProperty(name);
				}
			}
		};
	}

	/**
	 * Remove all property sources carried over from the parent environment. Add some empty sources where Spring boot
	 * expects them.
	 */
	private void clearPropertySources() {
		List<String> names = new ArrayList<String>();
		for (PropertySource<?> ps : getPropertySources()) {
			names.add(ps.getName());
		}
		for (String name : names) {
			getPropertySources().remove(name);
		}

		Map<String, Object> empty = Collections.emptyMap();
		getPropertySources().addLast(new MapPropertySource(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, empty));
		getPropertySources().addLast(new MapPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, empty));

	}
}
