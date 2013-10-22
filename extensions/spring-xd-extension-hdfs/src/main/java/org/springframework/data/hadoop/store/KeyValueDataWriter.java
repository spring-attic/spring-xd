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
package org.springframework.data.hadoop.store;

import java.io.IOException;

/**
 * A {@code KeyValueDataWriter} is a logical representation of
 * a data writer implementation that writes a key value pair.
 *
 * @author Janne Valkealahti
 *
 * @param <T> the type of the key to write
 * @param <E> the type of an entity to write
 */
public interface KeyValueDataWriter<T, E> extends DataWriter<E> {

	/**
	 * Write an entity to a key.
	 *
	 * @throws IOException if an I/O error occurs
	 *
	 * @param key the key to write to
	 * @param entity the entity to write
	 */
	void write(T key, E entity) throws IOException;

}
