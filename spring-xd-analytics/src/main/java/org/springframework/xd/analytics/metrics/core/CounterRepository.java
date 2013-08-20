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

import org.springframework.xd.store.DomainRepository;

/**
 * Interface for repositories that now how to deal with {@link Counter}s.
 * 
 * @author Mark Pollack
 * 
 */
public interface CounterRepository extends MetricRepository<Counter>, DomainRepository<Counter, String> {

	/**
	 * Increment the given counter by one, creating it if it did not exist. Implementations which can do this atomically
	 * in one operation are encouraged to do so.
	 */
	public long increment(String name);

	/**
	 * Increment the given counter by the specified amount, creating it if it did not exist. Implementations which can
	 * do this atomically in one operation are encouraged to do so.
	 */
	public long increment(String name, long amount);

	/**
	 * Decrement the given counter, creating it if it did not exist. Implementations which can do this atomically in one
	 * operation are encouraged to do so.
	 */
	public long decrement(String name);

	/**
	 * Reset the given counter to zero.
	 */
	public void reset(String name);

}
