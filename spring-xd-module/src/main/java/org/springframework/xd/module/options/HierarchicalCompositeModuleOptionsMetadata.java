/*
 * Copyright 2014-2015 the original author or authors.
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

import static org.springframework.xd.module.core.CompositeModule.OPTION_SEPARATOR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.xd.module.core.CompositeModule;


/**
 * A composite {@link ModuleOptionsMetadata} made of several {@link ModuleOptionsMetadata}, each assigned to a name,
 * that will appear in a hierarchy. Used to represent options to a composite module.
 * 
 * @author Eric Bottard
 * @author Gary Russell
 */
public class HierarchicalCompositeModuleOptionsMetadata implements ModuleOptionsMetadata {

	private Map<String, ModuleOptionsMetadata> composedModuleDefinitions;

	private List<ModuleOption> options = new ArrayList<ModuleOption>();

	private final static String HIERARCHICAL_PROFILE_SEPARATOR = "#";

	public HierarchicalCompositeModuleOptionsMetadata(Map<String, ModuleOptionsMetadata> hierarchy) {
		this.composedModuleDefinitions = hierarchy;
		for (Entry<String, ModuleOptionsMetadata> entry : hierarchy.entrySet()) {
			for (ModuleOption option : entry.getValue()) {
				ModuleOption mo = new ModuleOption(entry.getKey() + OPTION_SEPARATOR + option.getName(),
						option.getDescription());
				mo.withDefaultValue(option.getDefaultValue()).withType(option.getType());
				options.add(mo);
			}
		}
	}

	@Override
	public Iterator<ModuleOption> iterator() {
		return options.iterator();
	}

	@Override
	public ModuleOptions interpolate(final Map<String, String> raw) throws BindException {

		final Map<String, ModuleOptions> delegates = new HashMap<String, ModuleOptions>();
		Map<String, Map<String, String>> valuesWithoutPrefix = new HashMap<String, Map<String, String>>();
		for (String prefix : composedModuleDefinitions.keySet()) {
			valuesWithoutPrefix.put(prefix, new HashMap<String, String>());
		}

		BindingResult bindingResult = new BeanPropertyBindingResult(this, "options");


		for (Entry<String, String> entry : raw.entrySet()) {
			String key = entry.getKey();
			int separator = key.indexOf(OPTION_SEPARATOR);
			if (separator == -1) {
				bindingResult.addError(new FieldError("options", key, String.format("unsupported option '%s'", key)));
				continue;
			}
			String prefix = key.substring(0, separator);
			String suffix = key.substring(separator + OPTION_SEPARATOR.length());
			Map<String, String> map = valuesWithoutPrefix.get(prefix);
			if (map == null) {
				bindingResult.addError(new FieldError("options", key, String.format("unsupported option '%s'", key)));
				continue;
			}
			map.put(suffix, entry.getValue());
		}

		if (bindingResult.hasErrors()) {
			throw new BindException(bindingResult);
		}

		for (String prefix : composedModuleDefinitions.keySet()) {
			Map<String, String> subMap = valuesWithoutPrefix.get(prefix);
			ModuleOptions interpolated = composedModuleDefinitions.get(prefix).interpolate(subMap);
			delegates.put(prefix, interpolated);
		}

		return new ModuleOptions() {

			@Override
			public EnumerablePropertySource<?> asPropertySource() {
				Map<String, EnumerablePropertySource<?>> pss = new HashMap<String, EnumerablePropertySource<?>>();
				for (Entry<String, ModuleOptions> entry : delegates.entrySet()) {
					pss.put(entry.getKey(), entry.getValue().asPropertySource());
				}
				return new HierarchicalEnumerablePropertySource("foo", pss);
			}

			/**
			 * The composed module itself never needs to activate profiles. It's the submodules that should. The keys
			 * returned here are special constructs that are decoded by {@link PrefixNarrowingModuleOptions}.
			 */
			@Override
			public String[] profilesToActivate() {
				List<String> result = new ArrayList<String>(delegates.size());
				for (String prefix : delegates.keySet()) {
					ModuleOptions delegate = delegates.get(prefix);
					for (String p : delegate.profilesToActivate()) {
						result.add(qualifyProfile(prefix, p));
					}
				}
				return result.toArray(new String[result.size()]);
			}

			@Override
			public void validate() {
				for (ModuleOptions delegate : delegates.values()) {
					delegate.validate();
				}
			}
		};
	}

	/* default */static String qualifyProfile(String prefix, String profile) {
		return String.format("%s%s%s", prefix, HIERARCHICAL_PROFILE_SEPARATOR, profile);
	}

	/* default */static String[] filterQualifiedProfiles(String[] whole, String moduleName) {
		List<String> result = new ArrayList<String>();
		for (String raw : whole) {
			String prefix = moduleName + HIERARCHICAL_PROFILE_SEPARATOR;
			if (raw.startsWith(prefix)) {
				result.add(raw.substring(prefix.length()));
			}
		}
		return result.toArray(new String[result.size()]);
	}
}


class HierarchicalEnumerablePropertySource extends EnumerablePropertySource<Object> {

	private Map<String, EnumerablePropertySource<?>> delegates;

	public HierarchicalEnumerablePropertySource(String name, Map<String, EnumerablePropertySource<?>> delegates) {
		super(name, delegates);
		this.delegates = delegates;
	}

	@Override
	public String[] getPropertyNames() {
		Set<String> result = new HashSet<String>(delegates.size() * 3);
		for (String prefix : delegates.keySet()) {
			String[] sub = delegates.get(prefix).getPropertyNames();
			for (String value : sub) {
				result.add(prefix + CompositeModule.OPTION_SEPARATOR + value);
			}
		}
		return result.toArray(new String[result.size()]);
	}

	@Override
	public Object getProperty(String name) {
		int separator = name.indexOf(OPTION_SEPARATOR);
		if (separator == -1) {
			return null;
		}
		String prefix = name.substring(0, separator);
		EnumerablePropertySource<?> sub = delegates.get(prefix);
		if (sub == null) {
			return null;
		}
		return sub.getProperty(name.substring(separator + OPTION_SEPARATOR.length()));
	}

}
