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

import org.springframework.data.hadoop.store.support.KeyValueHolder;

/**
 * A {@code KeyValueDataReader} is a logical representation of
 * a data writer implementation that writes a key value pair.
 *
 * @author Janne Valkealahti
 *
 * @param <T> Type of a key for the reader
 * @param <E> Type of an entity for the reader
 */
public interface KeyValueEntityReader<T, E> extends EntityReader<E> {

	/**
	 * Read next key value.
	 *
	 * @return the {@code KeyValueHolder}
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	KeyValueHolder<T, E> readKeyValue() throws IOException;

}
