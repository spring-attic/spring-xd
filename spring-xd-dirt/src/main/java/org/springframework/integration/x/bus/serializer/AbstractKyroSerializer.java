/*
 *
 *  * Copyright 2013 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  * the License. You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  * specific language governing permissions and limitations under the License.
 *
 */

package org.springframework.integration.x.bus.serializer;

import java.io.IOException;
import java.io.OutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

/**
 * Base class for serializers using {@link com.esotericsoftware.kryo.Kryo}
 * 
 * @author David Turanski
 * @since 1.0
 */
abstract class AbstractKyroSerializer<T> {

	protected Kryo kryo;

	protected AbstractKyroSerializer() {
		kryo = new Kryo();
	}

	/**
	 * Serialize an object
	 * 
	 * @param object the object to be serialized
	 * @return a byte array representing the object in serialized form
	 * @throws IOException
	 */
	public byte[] serialize(T object) throws IOException {
		return serialize(object, null);
	}

	/**
	 * Serialize an object using an existing output stream
	 * 
	 * @param object the object to be serialized
	 * @param outputStream the output stream, e.g. a FileOutputStream
	 * @return a byte array representing the object in serialized form
	 * @throws IOException
	 */
	@SuppressWarnings("resource")
	public byte[] serialize(T object, OutputStream outputStream) throws IOException {
		Output output = (outputStream == null) ? new Output(2048, 16384) : new Output(outputStream);
		doSerialize(object, kryo, output);
		output.close();
		return output.getBuffer();
	}

	protected abstract void doSerialize(T object, Kryo kryo, Output output);

}
