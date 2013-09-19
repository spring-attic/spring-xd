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

package org.springframework.integration.x.bus.serializer.kryo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.integration.x.bus.serializer.MultiTypeDeserializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;

/**
 * Base class for Kryo serializers that handle multiple types
 * 
 * @author David Turanski
 * @since 1.0
 */
abstract class KryoMultiTypeSerializer<T> extends AbstractKyroSerializer<T> implements MultiTypeDeserializer<T> {

	/**
	 * Deserialize an object of a given type
	 * 
	 * @param inputStream the input stream containing the serialized object
	 * @param type the object's class
	 * @return the object
	 * @throws IOException
	 */
	@Override
	public T deserialize(InputStream inputStream, Class<? extends T> type) throws IOException {
		Input input = new Input(inputStream);
		T result = doDeserialize(getKryoInstance(), input, type);
		input.close();
		return result;
	}

	/**
	 * Deserialize an object of a given type
	 * 
	 * @param bytes the byte array containing the serialized object
	 * @param type the object's class
	 * @return the object
	 * @throws IOException
	 */
	@Override
	public T deserialize(byte[] bytes, Class<? extends T> type) throws IOException {
		return deserialize(new ByteArrayInputStream(bytes), type);
	}

	protected abstract T doDeserialize(Kryo kryo, Input input, Class<? extends T> type);
}
