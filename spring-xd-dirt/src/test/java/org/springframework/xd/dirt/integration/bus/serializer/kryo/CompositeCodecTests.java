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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.xd.dirt.integration.bus.serializer.AbstractCodec;
import org.springframework.xd.dirt.integration.bus.serializer.CompositeCodec;
import org.springframework.xd.dirt.integration.bus.serializer.MultiTypeCodec;
import org.springframework.xd.dirt.integration.bus.serializer.kryo.PojoCodec;
import org.springframework.xd.dirt.integration.bus.serializer.kryo.TupleCodec;
import org.springframework.xd.tuple.DefaultTuple;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;


/**
 * 
 * @author David Turanski
 */
public class CompositeCodecTests {

	private MultiTypeCodec<Object> codec;


	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Before
	public void setup() {
		Map<Class<?>, AbstractCodec<?>> codecs = new HashMap<Class<?>, AbstractCodec<?>>();
		codecs.put(Tuple.class, new TupleCodec());
		codec = new CompositeCodec(codecs, new PojoCodec());
	}

	@Test
	public void testPojoSerialization() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		SomeClassWithNoDefaultConstructors foo = new SomeClassWithNoDefaultConstructors("hello", 123);
		codec.serialize(foo, bos);
		SomeClassWithNoDefaultConstructors foo2 = (SomeClassWithNoDefaultConstructors) codec.deserialize(
				bos.toByteArray(),
				SomeClassWithNoDefaultConstructors.class);
		assertEquals(foo, foo2);
	}

	@Test
	public void testTupleSerialization() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		Tuple foo = TupleBuilder.tuple().of("hello", 123, "foo", "bar");
		codec.serialize(foo, bos);
		Tuple foo2 = (Tuple) codec.deserialize(
				bos.toByteArray(),
				DefaultTuple.class);
		// Not foo2.equals(foo) actually returns a new instance
		assertEquals(foo.getInt(0), foo2.getInt(0));
		assertEquals(foo.getString(1), foo2.getString(1));
	}

	static class SomeClassWithNoDefaultConstructors {

		private String val1;

		private int val2;

		public SomeClassWithNoDefaultConstructors(String val1) {
			this.val1 = val1;
		}

		public SomeClassWithNoDefaultConstructors(String val1, int val2) {
			this.val1 = val1;
			this.val2 = val2;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof SomeClassWithNoDefaultConstructors)) {
				return false;
			}
			SomeClassWithNoDefaultConstructors that = (SomeClassWithNoDefaultConstructors) other;
			return (this.val1.equals(that.val1) && val2 == that.val2);
		}
	}
}
