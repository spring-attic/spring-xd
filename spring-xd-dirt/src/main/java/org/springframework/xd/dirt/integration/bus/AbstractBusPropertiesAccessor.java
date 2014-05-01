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

package org.springframework.xd.dirt.integration.bus;

import java.util.Properties;

import org.springframework.util.StringUtils;


/**
 * Base class for bus-specific property accessors; common properies
 * are defined here.
 *
 * @author Gary Russell
 */
public abstract class AbstractBusPropertiesAccessor {

	private final Properties properties;

	public AbstractBusPropertiesAccessor(Properties properties) {
		if (properties == null) {
			this.properties = new Properties();
		}
		else {
			this.properties = properties;
		}
	}

	public String getProperty(String key) {
		return this.properties.getProperty(key);
	}

	public String getProperty(String key, String defaultValue) {
		return this.properties.getProperty(key, defaultValue);
	}

	public boolean getProperty(String key, boolean defaultValue) {
		String property = this.properties.getProperty(key);
		if (property != null) {
			return Boolean.parseBoolean(property);
		}
		else {
			return defaultValue;
		}
	}

	public int getProperty(String key, int defaultValue) {
		String property = this.properties.getProperty(key);
		if (property != null) {
			return Integer.parseInt(property);
		}
		else {
			return defaultValue;
		}
	}

	public long getProperty(String key, long defaultValue) {
		String property = this.properties.getProperty(key);
		if (property != null) {
			return Long.parseLong(property);
		}
		else {
			return defaultValue;
		}
	}

	public double getProperty(String key, double defaultValue) {
		String property = properties.getProperty(key);
		if (property != null) {
			return Double.parseDouble(property);
		}
		else {
			return defaultValue;
		}
	}

	public int getConcurrency(int defaultValue) {
		return getProperty("concurrency", defaultValue);
	}

	public int getMaxConcurrency(int defaultValue) {
		return getProperty("maxConcurrency", defaultValue);
	}

	// Retry properties

	public int getMaxAttempts(int defaultValue) {
		return getProperty("maxAttempts", defaultValue);
	}

	public long getBackOffInitialInterval(long defaultValue) {
		return getProperty("backOffInitialInterval", defaultValue);
	}

	public double getBackOffMultiplier(double defaultValue) {
		return getProperty("backOffMultiplier", defaultValue);
	}

	public long getBackOffMaxInterval(long defaultValue) {
		return getProperty("backOffMaxInterval", defaultValue);
	}

	// Utility methods

	protected String[] asStringArray(String value, String[] defaultValue) {
		if (StringUtils.hasText(value)) {
			return StringUtils.commaDelimitedListToStringArray(value);
		}
		else {
			return defaultValue;
		}
	}

	@Override
	public String toString() {
		return this.properties.toString();
	}

}
