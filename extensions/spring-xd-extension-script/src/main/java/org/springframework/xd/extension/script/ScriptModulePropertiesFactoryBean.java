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

package org.springframework.xd.extension.script;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author David Turanski
 */
public class ScriptModulePropertiesFactoryBean extends PropertiesFactoryBean {
	private String variables;

	public void setVariables(String variables) {
		this.variables = variables;
	}

	@Override
	protected Properties createProperties() throws IOException {
		Properties properties = mergeProperties();
		properties.putAll(parseVariables());
		return properties;
	}

	private Map<String, String> parseVariables() {
		Map<String, String> variableMap = new HashMap<String, String>();
		if (StringUtils.hasText(variables)) {
			String[] nameValues = variables.split(",");
			for (String nameValue : nameValues) {
				String[] tokens = nameValue.split("=");
				Assert.state(tokens.length == 2, "Invalid 'variables' format: " + variables);
				String name = tokens[0].trim();
				String value = tokens[1].trim();
				variableMap.put(name, value);
			}
		}
		return variableMap;
	}
}
