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

import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;
import org.springframework.util.StringValueResolver;

/**
 * A (fake) PlaceholderConfigurer that will collect the placeholders it encounters and derive a {@link ModuleOption} out
 * of them.
 * 
 * 
 * @author Eric Bottard
 */
public class DefaultModuleOptionsMetadataCollector extends PlaceholderConfigurerSupport {

	final PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper("${", "}");

	@Override
	protected void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props)
			throws BeansException {
		// no-op
	}

	public ModuleOptionsMetadata collect(ConfigurableListableBeanFactory beanFactory) {
		final SimpleModuleOptionsMetadata result = new SimpleModuleOptionsMetadata();

		final PlaceholderResolver placeholderResolver = new PlaceholderResolver() {

			@Override
			public String resolvePlaceholder(String placeholderName) {
				int colon = placeholderName.indexOf(':');
				String optionName = colon >= 0 ? placeholderName.substring(0, colon) : placeholderName;
				if (optionName.indexOf('.') == -1) {
					ModuleOption option = new ModuleOption(optionName, "unknown").withType(String.class);
					if (colon > 0) {
						String defaultValue = placeholderName.substring(colon + 1);
						if (!defaultValue.contains("${")) {
							// TODO: revisit (XD-1261)
							// option.withDefaultValue(defaultValue);
						}
					}
					result.add(option);
				}
				return placeholderName;
			}
		};


		final StringValueResolver resolver = new StringValueResolver() {

			@Override
			public String resolveStringValue(String strVal) {
				helper.replacePlaceholders(strVal, placeholderResolver);
				return strVal;
			}
		};
		doProcessProperties(beanFactory, resolver);

		return result;
	}

}
