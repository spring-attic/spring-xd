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

import java.io.Closeable;
import java.io.IOException;

/**
 * A {@code DataReader} is a logical representation of
 * data reader implementation.
 *
 * @author Janne Valkealahti
 *
 * @param <E> Type of an entity for the reader
 */
public interface DataReader<E> extends Closeable {

	/**
	 * Opens this reader and any system resources associated
	 * with it. If the reader is already opened then invoking this
	 * method has no effect.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	void open() throws IOException;

	/**
	 * Read next entity from a reader.
	 *
	 * @return the entity or <code>null</code>
	 * @throws IOException if an I/O error occurs
	 */
	E read() throws IOException;

}
