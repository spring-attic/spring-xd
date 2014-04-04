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

import java.util.HashSet;
import java.util.Set;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.xd.module.core.CompositeModule;


/**
 * A decorator on another instance of {@link ModuleOptions} (in practice, one representing options to a composed module)
 * that will filter and remap property values exposed through its property source. Only values starting with a given
 * prefix are exposed (without the prefix).
 * 
 * @author Eric Bottard
 */
public class PrefixNarrowingModuleOptions extends ModuleOptions {

	private final ModuleOptions composedModuleOptions;

	private final String moduleName;

	public PrefixNarrowingModuleOptions(ModuleOptions composedModuleOptions, String moduleName) {
		super();
		this.composedModuleOptions = composedModuleOptions;
		this.moduleName = moduleName;
	}

	@Override
	public String[] profilesToActivate() {
		String[] encoded = composedModuleOptions.profilesToActivate();
		return HierarchicalCompositeModuleOptionsMetadata.filterQualifiedProfiles(encoded, moduleName);
	}

	@Override
	public EnumerablePropertySource<?> asPropertySource() {
		final EnumerablePropertySource<?> ps = composedModuleOptions.asPropertySource();

		final String prefix = moduleName + CompositeModule.OPTION_SEPARATOR;

		return new EnumerablePropertySource<Object>(this.toString(), composedModuleOptions) {

			@Override
			public String[] getPropertyNames() {
				Set<String> result = new HashSet<String>();
				for (String name : ps.getPropertyNames()) {
					if (name.startsWith(prefix)) {
						result.add(name.substring(prefix.length()));
					}
				}
				return result.toArray(new String[result.size()]);
			}

			@Override
			public Object getProperty(String name) {
				return ps.getProperty(prefix + name);
			}

			@Override
			public boolean containsProperty(String name) {
				return ps.containsProperty(prefix + name);
			}
		};
	}

}
