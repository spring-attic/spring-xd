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
 * Represents the data stored in a counter of a single value.  The name property is a friendly user assigned name, and
 * should be unique across all counters that are created.
 * 
 * Note: Additional metadata to help in searching for Counters, such as tags and last time updated will be coming.
 * 
 * @author Mark Pollack
 *
 */
public final class Counter {

	private final String name;
	private final long count;
	
	/**
	 * Construct a new Counter given a name
	 * @param name the name of the counter.
	 */
	public Counter(String name) {
		Assert.notNull(name);
		this.name = name;
		this.count = 0L;
	}
	
	/**
	 * Construct a new Counter given a name and a initial value of the count
	 * @param name the name of the counter
	 * @param count initial value.
	 */
	@PersistenceConstructor
	public Counter(String name, long count) {
		Assert.notNull(name);
		this.name = name;
		this.count = count;
	}

	/**
	 * @return the count
	 */
	public long getCount() {
		return count;
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
		result = prime * result + (int) (count ^ (count >>> 32));
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
		if (!(obj instanceof Counter)) {
			return false;
		}
		Counter other = (Counter) obj;
		if (count != other.count) {
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
		return "Counter [name=" + name + ", count=" + count + "]";
	}

	
	

}
