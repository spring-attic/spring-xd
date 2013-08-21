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

package org.springframework.xd.analytics.metrics.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.util.Assert;

/**
 * Represents the data stored in a Counter for multiple field-value pairs. Operations on it are expected to increment or
 * decrement the value associated with a specific field name. The name property is a friendly user assigned name, and
 * should be unique.
 * 
 * @author Mark Pollack
 * 
 */
public final class FieldValueCounter implements Metric {

	private final String name;

	private final Map<String, Double> fieldValueCount;

	public FieldValueCounter(String name) {
		Assert.notNull(name);
		this.name = name;
		this.fieldValueCount = new ConcurrentHashMap<String, Double>();
	}

	@PersistenceConstructor
	public FieldValueCounter(String name, Map<String, Double> fieldValueCount) {
		Assert.notNull(name);
		Assert.notNull(fieldValueCount);
		this.name = name;
		this.fieldValueCount = fieldValueCount;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the fieldValueCount
	 */
	public Map<String, Double> getFieldValueCount() {
		return this.fieldValueCount;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "FieldValueCounter [name=" + name + ", fieldValueCount="
				+ fieldValueCount + "]";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof FieldValueCounter)) {
			return false;
		}
		FieldValueCounter other = (FieldValueCounter) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		}
		else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

}
