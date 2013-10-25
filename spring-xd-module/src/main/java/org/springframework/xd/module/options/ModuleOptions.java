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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.context.expression.MapAccessor;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;


/**
 * Encapsulates metadata about the accepted options for a module.
 * 
 * @author Eric Bottard
 */
public class ModuleOptions implements Iterable<ModuleOption> {

	/**
	 * A {@link ModuleOptions} stub that can be used when metadata is not provided. Allows handling that case as the
	 * normal case. Will report no declared options, will pass injected properties as is. Client code can test for
	 * equality to this constant when they need to really distinguish between zero options and no information available.
	 */
	public static ModuleOptions ABSENT = new ModuleOptions() {

		@Override
		public ModuleOptionsValues interpolate(final Properties raw) {
			return new ModuleOptionsValues(raw) {

				@Override
				public EnumerablePropertySource<?> asPropertySource() {
					return new PropertiesPropertySource("ModuleOptionsValues@"
							+ System.identityHashCode(this), raw);
				}
			};
		}
	};


	/**
	 * Represents runtime information about the module options once user provided values are injected.
	 * 
	 * @author Eric Bottard
	 */
	public class ModuleOptionsValues {

		private final StandardEvaluationContext context;

		private final SpelExpressionParser parser = new SpelExpressionParser();;


		private Properties raw;

		public ModuleOptionsValues(Properties raw) {
			this.raw = raw;
			context = new StandardEvaluationContext();
			context.addPropertyAccessor(new MapAccessor());
			context.addPropertyAccessor(new PropertySourcePropertyAccessor());
		}

		public EnumerablePropertySource<?> asPropertySource() {

			return new EnumerablePropertySource<ModuleOptionsValues>("ModuleOptionsValues@"
					+ System.identityHashCode(this),
					this) {

				@Override
				public Object getProperty(String name) {
					boolean isDeclared = options.containsKey(name);
					if (!isDeclared) {
						// Not an explicitly allowed property: move on
						return null;
					}
					ModuleOption moduleOption = options.get(name);
					Class<?> expectedType = moduleOption.getType();
					if (raw.containsKey(name)) {
						// Provided by user. Coerse String value to given type
						String asString = quote(raw.getProperty(name));
						return parser.parseExpression(asString).getValue(expectedType);
					}
					else {
						// Maybe compute default
						String defaultExpression = moduleOption.getSpel();
						if (defaultExpression == null) {
							return null;
						}
						// Allow evaluation against ourself
						return parser.parseExpression(defaultExpression).getValue(context, this, expectedType);
					}
				}

				private String quote(String property) {
					StringBuilder sb = new StringBuilder(property.length());
					sb.append("'");
					for (int i = 0; i < property.length(); i++) {
						if ('\'' == property.charAt(i)) {
							sb.append('\'');
						}
						sb.append(property.charAt(i));
					}
					sb.append("'");
					return sb.toString();
				}

				@Override
				public String[] getPropertyNames() {
					return options.keySet().toArray(EMPTY_NAMES_ARRAY);
				}
			};
		}

		public Set<String> getActivatedProfiles() {
			Set<String> result = new HashSet<>();
			for (Map.Entry<String, String> e : profileConditions.entrySet()) {
				try {
					if (parser.parseExpression(e.getValue()).getValue(context, raw, Boolean.class)) {
						result.add(e.getKey());
					}
				}
				catch (SpelEvaluationException ignored) {

				}
			}
			return result;
		}

	}

	private Map<String, ModuleOption> options = new HashMap<String, ModuleOption>();

	/**
	 * Setter-style class for specifying profile activation rules.
	 */
	public static class ProfileActivationRule {

		private String profile;

		private String rule;


		public void setProfile(String profile) {
			this.profile = profile;
		}

		public void setRule(String rule) {
			this.rule = rule;
		}


	}


	// ProfileName -> SpEL expression
	private Map<String, String> profileConditions = new HashMap<String, String>();

	public void add(ModuleOption option) {
		options.put(option.getName(), option);
	}

	public void setProfileRules(List<ProfileActivationRule> rules) {
		profileConditions.clear();
		for (ProfileActivationRule r : rules) {
			addProfileActivationRule(r.profile, r.rule);
		}
	}

	public void setOptions(List<ModuleOption> options) {
		this.options.clear();
		for (ModuleOption o : options) {
			add(o);
		}
	}

	/**
	 * Declare that some profile should be activated if the given expression evaluates to true.
	 */
	public void addProfileActivationRule(String profileName, String expression) {
		profileConditions.put(profileName, expression);
	}

	public ModuleOptionsValues interpolate(Properties raw) {
		return new ModuleOptionsValues(raw);
	}

	@Override
	public Iterator<ModuleOption> iterator() {
		return options.values().iterator();
	}

	@Override
	public String toString() {
		return String.format("ModuleOptions[options = %s, profiles = %s]", options.keySet(), profileConditions.keySet());
	}
}
