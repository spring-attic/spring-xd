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

import org.apache.hadoop.fs.Path;

/**
 * Interface for a generic concept of a Storage.
 * 
 * @author Janne Valkealahti
 * 
 */
public interface Storage extends Closeable {

	/**
	 * Gets the data writer.
	 * 
	 * @return the data writer
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	DataWriter getDataWriter() throws IOException;

	/**
	 * Gets the data reader.
	 * 
	 * @return the data reader
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	DataReader getDataReader(Path path) throws IOException;

}
