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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.core.env.PropertySource;


/**
 * An implementation of {@link ModuleOptions} that only knows how to list options and does not support advanced
 * facilities such as derived options, profile activation or validation.
 * 
 * @author Eric Bottard
 */
public class SimpleModuleOptions implements ModuleOptions {

	private Map<String, ModuleOption> options = new LinkedHashMap<String, ModuleOption>();

	public void add(ModuleOption option) {
		options.put(option.getName(), option);
	}

	@Override
	public Iterator<ModuleOption> iterator() {
		return options.values().iterator();
	}

	@Override
	public InterpolatedModuleOptions interpolate(final Properties raw) {
		return new InterpolatedModuleOptions() {

			@Override
			public PropertySource<?> asPropertySource() {
				// Return a property source with only the intersection
				// between declared properties and actual provided values
				return new PropertySource<Object>(this.toString()) {

					@Override
					public Object getProperty(String name) {
						ModuleOption option = options.get(name);
						if (option != null) {
							String provided = raw.getProperty(name);
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
				};
			}
		};
	}
}
