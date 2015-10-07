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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.codec.Codec;
import org.springframework.integration.codec.kryo.PojoCodec;
import org.springframework.xd.tuple.DefaultTuple;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * @author David Turanski
 * @author Gary Russell
 */
public class TupleCodecTests {

	private Codec codec;

	@SuppressWarnings({ "rawtypes" })
	@Before
	public void setup() {
		codec = new PojoCodec(new TupleKryoRegistrar());
	}

	@Test
	public void testNestedTupleSerialization() throws IOException {
		Tuple t0 = TupleBuilder.tuple().of("one", 1, "two", 2);
		Tuple t1 = TupleBuilder.tuple().of("three", 3, "four", 4, "t0", t0);
		Tuple t2 = TupleBuilder.tuple().of("t1", t1);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		codec.encode(t2, bos);
		Tuple t3 = codec.decode(bos.toByteArray(), DefaultTuple.class);
		Tuple t4 = (Tuple) t3.getValue("t1");
		Tuple t5 = (Tuple) t4.getValue("t0");
		assertEquals(1, t5.getInt("one"));
		assertEquals(2, t5.getInt("two"));
		assertEquals(t0, t5);
	}

	@Test
	public void testTupleSerialization() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		Tuple foo = TupleBuilder.tuple().of("hello", 123, "foo", "bar");
		codec.encode(foo, bos);

		Tuple foo2 = codec.decode(
				bos.toByteArray(),
				DefaultTuple.class);
		// Not foo2.equals(foo) actually returns a new instance
		assertEquals(foo.getInt(0), foo2.getInt(0));
		assertEquals(foo.getString(1), foo2.getString(1));
	}
}
