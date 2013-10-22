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
package org.springframework.data.hadoop.store.support;

/**
 * A {@code KeyValueHolder} is used to return multiple values
 * from a function, in this case a key and a value. Other use
 * case is to denote null values in cases where i.e. the stream
 * reader itself might return null indicating end of stream.
 *
 * @author Janne Valkealahti
 *
 * @param <T> Type of a key
 * @param <E> Type of an entity
 */
public class KeyValueHolder<T, E> {

	T key;
	E value;

	/**
	 * Instantiates a new key value holder.
	 */
	public KeyValueHolder() {}

	/**
	 * Instantiates a new key value holder.
	 *
	 * @param key the key
	 * @param value the value
	 */
	public KeyValueHolder(T key, E value) {
		this.key = key;
		this.value = value;
	}

	/**
	 * Gets the key.
	 *
	 * @return the key
	 */
	public T getKey() {
		return key;
	}

	/**
	 * Sets the key.
	 *
	 * @param key the new key
	 */
	public void setKey(T key) {
		this.key = key;
	}

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public E getValue() {
		return value;
	}

	/**
	 * Sets the value.
	 *
	 * @param value the new value
	 */
	public void setValue(E value) {
		this.value = value;
	}

}
