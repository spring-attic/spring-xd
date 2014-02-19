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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.validation.BindException;
import org.springframework.xd.module.ModuleDefinition;


/**
 * A decorator around another {@link ModuleOptionsMetadataResolver} that will provide default values for module options
 * using the environment.
 * 
 * <p>
 * For each option {@code <optionname>} of a module (of type {@code <type>} and name {@code <modulename>}), this
 * resolver will try to read a default from {@code <type>.<modulename>.<optionname>}.
 * 
 * @author Eric Bottard
 */
public class DefaultsProvidingModuleOptionsMetadataResolver implements ModuleOptionsMetadataResolver, EnvironmentAware {

	private final ModuleOptionsMetadataResolver delegate;

	private Environment environment;

	public DefaultsProvidingModuleOptionsMetadataResolver(ModuleOptionsMetadataResolver delegate) {
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
			for (ModuleOption original : wrapped) {
				Object newDefault = computeDefault(original);
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

		private Object computeDefault(ModuleOption option) {
			String fqKey = String.format("%s.%s.%s", moduleDefinition.getType(), moduleDefinition.getName(),
					option.getName());
			return environment.getProperty(fqKey);
		}

	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

}
