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

package org.springframework.xd.tuple.serializer.kryo;

import java.util.ArrayList;
import java.util.UUID;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import org.springframework.xd.dirt.integration.bus.serializer.kryo.AbstractKryoCodec;
import org.springframework.xd.tuple.DefaultTuple;
import org.springframework.xd.tuple.Tuple;

/**
 * Kryo serializer for {@link Tuple}
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
		return kryo.readObject(input, DefaultTuple.class);
	}

	protected void configureKryoInstance(Kryo kryo) {
		kryo.register(DefaultTuple.class, new DefaultTupleSerializer(), TUPLE_REGISTRATION_ID);
		kryo.register(ArrayList.class, ARRAY_LIST_REGISTRATION_ID);
		kryo.register(UUID.class, UUID_REGISTRATION_ID);
		kryo.register(Long.class, LONG_REGISTRATION_ID);
	}

}
