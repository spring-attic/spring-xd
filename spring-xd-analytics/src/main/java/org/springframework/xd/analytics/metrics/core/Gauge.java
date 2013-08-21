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
	 * 
	 * @param name the name of the Gauge.
	 */
	public Gauge(String name) {
		Assert.notNull(name);
		this.name = name;
		this.value = 0L;
	}

	/**
	 * Construct a new Gauge given a name and a initial value of the value
	 * 
	 * @param name the name of the Gauge
	 * @param value initial value.
	 */
	@PersistenceConstructor
	public Gauge(String name, long value) {
		Assert.notNull(name);
		this.name = name;
		this.value = value;
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

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Gauge gauge = (Gauge) o;

		if (!name.equals(gauge.name))
			return false;

		return true;
	}

	@Override
	public String toString() {
		return "Gauge [name=" + name + ", value=" + value + "]";
	}

}
