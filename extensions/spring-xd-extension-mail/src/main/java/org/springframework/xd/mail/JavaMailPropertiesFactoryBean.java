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

package org.springframework.xd.mail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.beans.factory.config.PropertiesFactoryBean} that
 * reads comma separated (ex: key1=value1,key2=value2) values and make them as properties.
 *
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 */
public class JavaMailPropertiesFactoryBean extends PropertiesFactoryBean implements ResourceLoaderAware {

	private String defaultPropertiesString;

	private String propertiesFileString;

	private String propertiesString;

	private ResourceLoader resourceLoader;

	public void setPropertiesString(String propertiesString) {
		this.propertiesString = propertiesString;
	}

	public void setDefaultPropertiesString(String defaultPropertiesString) {
		this.defaultPropertiesString = defaultPropertiesString;
	}

	public void setPropertiesFileString(String propertiesFileString) {
		this.propertiesFileString = propertiesFileString;
	}

	@Override
	protected Properties createProperties() throws IOException {
		Assert.isTrue(propertiesFileString == null || propertiesString == null, "Only one of propertiesString and " +
				"propertiesLocation should be set.");
		Properties properties = new Properties();
		properties.putAll(parsePropertiesString(propertiesString));
		Properties propertiesFromFile = loadPropertiesFile();
		if (!propertiesFromFile.isEmpty()) {
			properties.putAll(propertiesFromFile);
		}
		if (StringUtils.hasText(defaultPropertiesString)) {
			Map<String, String> defaultProperties = parsePropertiesString(defaultPropertiesString);
			for (Map.Entry<String, String> entry : defaultProperties.entrySet()) {
				if (properties.getProperty(entry.getKey()) == null) {
					properties.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return properties;
	}

	private Map<String, String> parsePropertiesString(String propertiesString) {
		Map<String, String> variableMap = new HashMap<String, String>();
		if (StringUtils.hasText(propertiesString)) {
			String[] nameValues = propertiesString.split(",");
			for (String nameValue : nameValues) {
				String[] tokens = nameValue.split("=");
				Assert.state(tokens.length == 2, "Invalid 'properties' format: " + propertiesString);
				String name = tokens[0].trim();
				String value = tokens[1].trim();
				variableMap.put(name, value);
			}
		}
		return variableMap;
	}

	private Properties loadPropertiesFile() {
		Properties properties = new Properties();
		if (StringUtils.hasText(propertiesFileString)) {
			try {
				PropertiesLoaderUtils.fillProperties(properties,
						new EncodedResource(resourceLoader.getResource(propertiesFileString)));
			}
			catch (IOException e) {
				throw new RuntimeException("Error loading the properties from "+ propertiesFileString + e);
			}
		}
		return properties;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}
}
