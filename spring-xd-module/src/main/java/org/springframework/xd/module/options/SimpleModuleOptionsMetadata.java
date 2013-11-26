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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;


/**
 * An implementation of {@link ModuleOptionsMetadata} that only knows how to list options and does not support advanced
 * facilities such as derived options, profile activation or validation.
 * 
 * @author Eric Bottard
 */
public class SimpleModuleOptionsMetadata implements ModuleOptionsMetadata {

	private Map<String, ModuleOption> options = new LinkedHashMap<String, ModuleOption>();

	public void add(ModuleOption option) {
		options.put(option.getName(), option);
	}

	@Override
	public Iterator<ModuleOption> iterator() {
		return options.values().iterator();
	}

	@Override
	public ModuleOptions interpolate(final Map<String, String> raw) throws BindException {

		MapBindingResult bindingResult = new MapBindingResult(new HashMap<String, String>(), "options");
		for (String provided : raw.keySet()) {
			if (!options.containsKey(provided)) {
				bindingResult.addError(new FieldError("options", provided, String.format(
						"Module option '%s' does not exist", provided)));
			}
		}

		if (bindingResult.hasErrors()) {
			throw new BindException(bindingResult);
		}

		return new ModuleOptions() {

			@Override
			public EnumerablePropertySource<?> asPropertySource() {
				// Return a property source with only the intersection
				// between declared properties and actual provided values
				return new EnumerablePropertySource<Object>(this.toString(), this) {

					@Override
					public Object getProperty(String name) {
						ModuleOption option = options.get(name);
						if (option != null) {
							String provided = raw.get(name);
							if (provided != null) {
								return provided;
							}
							else {
								return option.getDefaultValue();
							}
						} // option is not in the allowed list
						else {
							return null;
						}
					}

					@Override
					public String[] getPropertyNames() {
						return options.keySet().toArray(new String[options.size()]);
					}
				};
			}
		};
	}
}
