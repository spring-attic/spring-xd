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
 * Represents the data stored in a valueer of a single value. Operations on it are expected to increment or decrement
 * the value. The name property is a friendly user assigned name, and should be unique.
 * 
 * Note: Additional metadata to help in searching for Counters, such as tags and last time updated will be coming.
 * 
 * @author Mark Pollack
 * 
 */
public class Counter implements Metric {

	private final String name;

	private long value;

	/**
	 * Construct a new Counter given a name
	 * 
	 * @param name the name of the Counter.
	 */
	public Counter(String name) {
		Assert.notNull(name);
		this.name = name;
		this.value = 0L;
	}

	/**
	 * Construct a new Counter given a name and a initial value of the value
	 * 
	 * @param name the name of the value
	 * @param value initial value.
	 */
	@PersistenceConstructor
	public Counter(String name, long value) {
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

	/**
	 * Increment this counter by a given amount. Stores that manage their own value bookkeepingmay not use this method.
	 */
	public long increment(long amount) {
		return value += amount;
	}

	/**
	 * Decrement this counter by a given amount. Stores that manage their own value bookkeepingmay not use this method.
	 */
	public long decrement(long amount) {
		return value -= amount;
	}

	/**
	 * @return the name
	 */
	@Override
	public String getName() {
		return name;
	}

	@Override
	public final int hashCode() {
		return name.hashCode();
	}

	@Override
	public final boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Counter)) {
			return false;
		}

		Counter counter = (Counter) o;

		if (!name.equals(counter.name))
			return false;

		return true;
	}

	@Override
	public String toString() {
		return "Counter [name=" + name + ", value=" + value + "]";
	}

}
