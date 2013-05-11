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

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.util.Assert;


/**
 * Represents the data stored in a Gauge which can be set to any integral value.
 *
 * The name property is a friendly user assigned name, and should be unique.
 *
 * Note: Additional metadata to help in searching for Gauges, such as tags and last time updated will be coming.
 *
 * @author Mark Pollack
 *
 */
public final class Gauge implements Metric {

	private final String name;
	private long value;

	/**
	 * Construct a new Gauge given a name
	 * @param name the name of the Gauge.
	 */
	public Gauge(String name) {
		Assert.notNull(name);
		this.name = name;
		this.value = 0L;
	}

	/**
	 * Construct a new Gauge given a name and a initial value of the value
	 * @param name the name of the Gauge
	 * @param count initial value.
	 */
	@PersistenceConstructor
	public Gauge(String name, long count) {
		Assert.notNull(name);
		this.name = name;
		this.value = count;
	}

	/**
	 * @return the value
	 */
	public long getValue() {
		return value;
	}

	Gauge set(long value) {
		this.value = value;
		return this;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (value ^ (value >>> 32));
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/* (non-Javadoc)
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
		if (!(obj instanceof Gauge)) {
			return false;
		}
		Gauge other = (Gauge) obj;
		if (value != other.value) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Gauge [name=" + name + ", value=" + value + "]";
	}

}

