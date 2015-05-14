/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.tuple.serializer.kryo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * @author David Turanski
 */
public class DefaultTupleSerializer extends Serializer<Tuple> {
	@Override
	public void write(Kryo kryo, Output output, Tuple tuple) {
		kryo.writeObject(output, tuple.getFieldNames());
		for (Object val: tuple.getValues()) {
			kryo.writeClassAndObject(output, val);
		}
		kryo.writeObjectOrNull(output, tuple.getId(), UUID.class);
		kryo.writeObjectOrNull(output, tuple.getTimestamp(), Long.class);
	}

	@Override
	public Tuple read(Kryo kryo, Input input, Class<Tuple> type) {
		List<String> names = kryo.readObject(input, ArrayList.class);
		List<Object> values = new ArrayList<>(names.size());
		for (int i = 0; i < names.size(); i++) {
			Object val = kryo.readClassAndObject(input);
			values.add(i, val);
		}
		UUID id = kryo.readObjectOrNull(input, UUID.class);
		Long timestamp = kryo.readObjectOrNull(input, Long.class);
		return createTupleInstance(names, values, id, timestamp);
	}

	private Tuple createTupleInstance(List<String> names, List<Object> values, UUID id, Long timestamp) {
		TupleBuilder tupleBuilder = TupleBuilder.tuple();
		// These values not preserved but added to the instance if non null to emulate immutability
		if (id != null) {
			tupleBuilder.addId();
		}
		if (timestamp != null) {
			tupleBuilder.addTimestamp();
		}

		return tupleBuilder.ofNamesAndValues(names, values);
	}
}
