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
package org.springframework.xd.analytics.metrics.service;

import org.springframework.xd.analytics.metrics.core.Counter;


/**
 * A Counter service that allows you to increment and decrement the count by one.  You can also reset the counter.
 * 
 * It also provides a 'getOrCreate' method that will return an existing counter given the name or create a new
 * counter set to zero.
 * 
 * @author Mark Pollack
 *
 */
public interface CounterService {

	/**
	 * Returns an existing counter for the given name or creates a new counter set to zero
	 * @param name name of the counter
	 * @return existing or new counter.
	 * @throws IllegalArgumentException in case the given name is null
	 */
	Counter getOrCreate(String name);
	
	/**
	 * Increment the counter by one
	 * @param name the counter name
	 * @throws IllegalArgumentException in case the given name is null
	 */
	void increment(String name);

	/**
	 * Decrement the counter by one
	 * @param name the counter name
	 * @throws IllegalArgumentException in case the given name is null
	 */
	void decrement(String name);

	/**
	 * Reset the counter to zero
	 * @param name the counter name
	 * @throws IllegalArgumentException in case the given name is null
	 */
	void reset(String name);
	
	//Consider overloads that take Counter instances
	
}
