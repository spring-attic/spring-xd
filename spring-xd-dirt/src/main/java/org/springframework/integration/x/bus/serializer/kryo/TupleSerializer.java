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

import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo serializer for {@link Tuple}
 * 
 * @author David Turanski
 * @since 1.0
 */
public class TupleSerializer extends KryoSingleTypeSerializer<Tuple> {

	@Override
	protected void doSerialize(Tuple object, Kryo kryo, Output output) {
		output.writeString(object.toString());
	}

	@Override
	protected Tuple doDeserialize(Kryo kryo, Input input) {
		return TupleBuilder.fromString(input.readString());
	}
}
