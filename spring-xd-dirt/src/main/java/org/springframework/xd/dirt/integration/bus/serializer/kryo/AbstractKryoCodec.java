/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.integration.bus.serializer.kryo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.util.Assert;
import org.springframework.xd.dirt.integration.bus.serializer.AbstractCodec;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;


/**
 * 
 * @author David Turanski
 */
abstract class AbstractKryoCodec<T> extends AbstractCodec<T> {

	/**
	 * Serialize an object using an existing output stream
	 * 
	 * @param object the object to be serialized
	 * @param outputStream the output stream, e.g. a FileOutputStream
	 * @throws IOException
	 */
	@Override
	public void serialize(T object, OutputStream outputStream) throws IOException {
		Assert.notNull(outputStream, "outputStream cannot be null");
		Output output = new Output(outputStream);
		doSerialize(object, getKryoInstance(), output);
		output.close();
	}

	/**
	 * Deserialize an object when the type is known
	 * 
	 * @param inputStream the input stream containing the serialized object
	 * @return the object
	 * @throws IOException
	 */
	@Override
	public T deserialize(InputStream inputStream) throws IOException {
		Input input = new Input(inputStream);
		T result = doDeserialize(getKryoInstance(), input);
		input.close();
		return result;
	}

	/**
	 * Deserialize an object when the type is known
	 * 
	 * @param bytes the byte array containing the serialized object
	 * @return the object
	 * @throws IOException
	 */
	@Override
	public T deserialize(byte[] bytes) throws IOException {
		return deserialize(new ByteArrayInputStream(bytes));
	}

	protected Kryo getKryoInstance() {
		return new Kryo();
	}

	protected abstract void doSerialize(T object, Kryo kryo, Output output);

	protected abstract T doDeserialize(Kryo kryo, Input input);
}
