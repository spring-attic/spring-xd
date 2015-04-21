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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.xd.tuple.DefaultTuple;
import org.springframework.xd.tuple.DefaultTupleConversionService;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleToJsonStringConverter;

/**
 * Kryo serializer for {@link Tuple}
 *
 * @author David Turanski
 * @since 1.0
 */
public class TupleCodec extends AbstractKryoCodec<Tuple> {

	@Override
	protected void doSerialize(Kryo kryo, Tuple object, Output output) {
		kryo.writeObject(output, object);
	}

	@Override
	protected Tuple doDeserialize(Kryo kryo, Input input) {
		DefaultTuple tuple = kryo.readObject(input, DefaultTuple.class);
		restoreConversionService(tuple);
		return tuple;
	}

	/**
	 * @param tuple
	 */
	private void restoreConversionService(DefaultTuple tuple) {
		setConversionService(tuple, new DefaultTupleConversionService(), new TupleToJsonStringConverter());
		for (Object value : tuple.getValues()) {
			if (value instanceof DefaultTuple) {
				restoreConversionService((DefaultTuple) value);
			}
		}
	}

	/**
	 * @param defaultTupleConversionService
	 * @param tupleToJsonStringConverter
	 */
	private void setConversionService(DefaultTuple tuple, DefaultTupleConversionService defaultTupleConversionService,
			TupleToJsonStringConverter tupleToJsonStringConverter) {
		DirectFieldAccessor dfa = new DirectFieldAccessor(tuple);
		dfa.setPropertyValue("configurableConversionService", defaultTupleConversionService);
		dfa.setPropertyValue("tupleToStringConverter", tupleToJsonStringConverter);
	}

	protected void configureKryoInstance(Kryo kryo) {
		kryo.register(DefaultTuple.class, TUPLE_REGISTRATION_ID);
	}
}
