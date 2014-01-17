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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;

/**
 * A composite {@link ModuleOptionsMetadata} made of several {@link ModuleOptionsMetadata} that will appear "flattened".
 * 
 * @author Eric Bottard
 */
public class FlattenedCompositeModuleOptionsMetadata implements ModuleOptionsMetadata {

	private static final Log logger = LogFactory.getLog(FlattenedCompositeModuleOptionsMetadata.class);

	private Map<String, ModuleOption> options = new LinkedHashMap<String, ModuleOption>();

	private Map<ModuleOptionsMetadata, Set<String>> momToSupportedOptions = new LinkedHashMap<ModuleOptionsMetadata, Set<String>>();

	public FlattenedCompositeModuleOptionsMetadata(List<? extends ModuleOptionsMetadata> delegates) {
		for (final ModuleOptionsMetadata delegate : delegates) {
			Set<String> optionNames = new HashSet<String>();
			momToSupportedOptions.put(delegate, optionNames);
			for (ModuleOption option : delegate) {
				if (options.put(option.getName(), option) != null) {
					throw new IllegalArgumentException(String.format("Module option named %s is already present",
							option.getName()));
				}
				optionNames.add(option.getName());
			}
		}
	}

	@Override
	public Iterator<ModuleOption> iterator() {
		return options.values().iterator();
	}

	@Override
	public ModuleOptions interpolate(Map<String, String> raw) throws BindException {


		@SuppressWarnings("serial")
		Map<ModuleOptionsMetadata, Map<String, String>> distributed = new HashMap<ModuleOptionsMetadata, Map<String, String>>() {

			@Override
			public Map<String, String> get(Object key) {
				Map<String, String> result = super.get(key);
				if (result == null) {
					result = new HashMap<String, String>();
					this.put((ModuleOptionsMetadata) key, result);
				}
				return result;
			}
		};

		Map<String, String> copy = new HashMap<String, String>(raw);

		// First, distribute raw input values to their corresponding MOM
		for (String key : raw.keySet()) {
			for (ModuleOptionsMetadata mom : momToSupportedOptions.keySet()) {
				Set<String> optionNames = momToSupportedOptions.get(mom);
				if (optionNames.contains(key)) {
					distributed.get(mom).put(key, copy.remove(key));
					break;
				}
			}
		}

		if (copy.size() > 0) {
			// We're just interested in a container for errors
			BindingResult bindingResult = new MapBindingResult(new HashMap<String, Object>(), "flattened");
			for (String pty : copy.keySet()) {
				bindingResult.addError(new FieldError("flattened", pty, String.format(
						"option named '%s' is not supported", pty)));
			}
			throw new BindException(bindingResult);
		}

		// Then, interpolate per-MOM and remember which MOM is responsible for which exposed value
		final Map<String, ModuleOptions> nameToOptions = new HashMap<String, ModuleOptions>();

		for (ModuleOptionsMetadata mom : momToSupportedOptions.keySet()) {
			Map<String, String> rawValuesSubset = distributed.get(mom);
			ModuleOptions mo = mom.interpolate(rawValuesSubset);
			for (String optionName : mo.asPropertySource().getPropertyNames()) {
				nameToOptions.put(optionName, mo);
			}
		}

		final Set<ModuleOptions> uniqueModuleOptions = new HashSet<ModuleOptions>(nameToOptions.values());

		return new ModuleOptions() {

			@Override
			public EnumerablePropertySource<FlattenedCompositeModuleOptionsMetadata> asPropertySource() {
				String psName = String.format("flattened-%d",
						System.identityHashCode(FlattenedCompositeModuleOptionsMetadata.this));
				return new EnumerablePropertySource<FlattenedCompositeModuleOptionsMetadata>(psName,
						FlattenedCompositeModuleOptionsMetadata.this) {

					@Override
					public String[] getPropertyNames() {
						String[] result = nameToOptions.keySet().toArray(new String[nameToOptions.keySet().size()]);
						FlattenedCompositeModuleOptionsMetadata.logger.debug(String.format(
								"%s returning propertyNames: %s", FlattenedCompositeModuleOptionsMetadata.logger,
								result));
						return result;
					}

					@Override
					public Object getProperty(String name) {
						ModuleOptions moduleOptions = nameToOptions.get(name);
						return moduleOptions == null ? null : moduleOptions.asPropertySource().getProperty(name);
					}
				};
			}

			@Override
			public String[] profilesToActivate() {
				List<String> result = new ArrayList<String>();
				for (ModuleOptions delegate : uniqueModuleOptions) {
					result.addAll(Arrays.asList(delegate.profilesToActivate()));
				}
				return result.toArray(new String[result.size()]);
			}

			@Override
			public void validate() {
				for (ModuleOptions delegate : uniqueModuleOptions) {
					delegate.validate();
				}
			}
		};
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		for (ModuleOptionsMetadata sub : momToSupportedOptions.keySet()) {
			sb.append("\n").append(momToSupportedOptions.get(sub)).append(" => ").append(sub);
		}
		return sb.toString();
	}
}
