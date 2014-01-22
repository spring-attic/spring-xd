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

package org.springframework.xd.shell.properties;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Holder object for {@code Properties}s objects.
 * 
 * @author Janne Valkealahti
 */
public class SpringConfigurationProperties {

	/** Properties considered as global */
	private final Properties globalProperties;

	/** Properties map identified by an id */
	private final Map<String, Properties> taggedProperties;

	/**
	 * Instantiates a new spring configuration properties.
	 */
	public SpringConfigurationProperties() {
		this.globalProperties = new Properties();
		this.taggedProperties = new HashMap<String, Properties>();
	}

	/**
	 * Gets the global properties.
	 * 
	 * @return the global properties
	 */
	public Properties getProperties() {
		return globalProperties;
	}

	/**
	 * Gets the properties.
	 * 
	 * @param id the id
	 * @return the properties
	 */
	public Properties getProperties(String id) {
		return taggedProperties.get(id);
	}

	public Map<String, Properties> getTaggedProperties() {
		return taggedProperties;
	}

	public void setProperty(String key, String value) {
		setProperty(null, key, value);
	}

	public synchronized void setProperty(String id, String key, String value) {
		if (StringUtils.hasText(id)) {
			Properties properties = taggedProperties.get(id);
			if (properties == null) {
				properties = new Properties();
				taggedProperties.put(id, properties);
			}
			properties.put(key, value);
		}
		else {
			globalProperties.setProperty(key, value);
		}
	}

	public Properties getMergedProperties(String id) {
		Properties properties = new Properties();
		CollectionUtils.mergePropertiesIntoMap(globalProperties, properties);
		CollectionUtils.mergePropertiesIntoMap(taggedProperties.get(id), properties);
		return properties;
	}

}
