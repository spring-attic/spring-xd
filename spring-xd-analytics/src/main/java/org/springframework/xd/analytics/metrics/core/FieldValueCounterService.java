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


/**
 * A Counter service that allows you to increment and decrement the count by one for a specific field.  You can also reset the counter
 * of a specific field.
 *
 * It also provides a 'getOrCreate' method that will return an existing counter given the name or create a new
 * counter set to zero.
 *
 * @author Mark Pollack
 *
 */
public interface FieldValueCounterService {

	/**
	 * Returns an existing FieldValueCounter for the given name or creates a new FieldValueCounter with the counts of all 
	 * fields set to zero
	 * @param name name of the FieldValueCounter
	 * @return existing or new FieldValueCounter.
	 * @throws IllegalArgumentException in case the given name is null
	 */
	FieldValueCounter getOrCreate(String name);

	/**
	 * Increment the FieldValueCounter for a given field name by one
	 * @param name the FieldValueCounter name
	 * @param fieldName the name of the field
	 * @throws IllegalArgumentException in case the given name is null
	 */
	void increment(String name, String fieldName);

	/**
	 * Decrement the FieldValueCounter for a given field name by one
	 * @param name the FieldValueCounter name
	 * @param fieldName the name of the field
	 * @throws IllegalArgumentException in case the given name is null
	 */
	void decrement(String name, String fieldName);
	
	/**
	 * Reset the FieldValueCounter to zero for the given field name
	 * @param name the FieldValueCounter name
	 * @param fieldName the name of the field
	 * @throws IllegalArgumentException in case the given name is null
	 */
	void reset(String name, String fieldName);


}
