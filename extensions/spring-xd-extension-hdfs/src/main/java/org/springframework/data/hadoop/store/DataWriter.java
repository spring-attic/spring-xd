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
import java.io.Flushable;
import java.io.IOException;

/**
 * Base {@code DataWriter} interface.
 * 
 * @author Janne Valkealahti
 * 
 */
public interface DataWriter extends Flushable, Closeable {

	/**
	 * Write next bytes into a data stream.
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	void write(byte[] value) throws IOException;

	/**
	 * Gets the position in a writer. Usually this will indicate how many bytes has been written into a stream. One need
	 * to understand that if underlying stream does its own buffering this method may not give accurate results until
	 * stream has been completely flushed. Mostly this will happen with compressed streams which may buffer data and
	 * thus wrapped stream will see nothing even if data has been written.
	 * 
	 * @return the position
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	long getPosition() throws IOException;

}
