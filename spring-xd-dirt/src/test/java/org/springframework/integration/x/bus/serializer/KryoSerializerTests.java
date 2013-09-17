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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Test;

import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * @author David Turanski
 * @since 1.0
 */
public class KryoSerializerTests {

	@Test
	public void testTupleSerialization() throws IOException {
		Tuple t = TupleBuilder.tuple().of("foo", "bar");
		TupleSerializer serializer = new TupleSerializer();

		byte[] bytes = serializer.serialize(t);

		Tuple t2 = serializer.deserialize(bytes);
		assertEquals(t, t2);
	}

	@Test
	public void testStringSerialization() throws IOException {
		String str = "hello";
		StringSerializer serializer = new StringSerializer();

		byte[] bytes = serializer.serialize(str);

		String s2 = serializer.deserialize(bytes);
		assertEquals(str, s2);
	}

	@Test
	public void testSerializationWithStreams() throws IOException {
		String str = "hello";
		File file = new File("test.ser");
		StringSerializer serializer = new StringSerializer();
		FileOutputStream fos = new FileOutputStream(file);
		serializer.serialize(str, fos);
		fos.close();

		FileInputStream fis = new FileInputStream(file);
		String s2 = serializer.deserialize(fis);
		file.delete();
		assertEquals(str, s2);
	}

	@Test
	public void testPojoSerialization() throws IOException {
		PojoSerializer serializer = new PojoSerializer();
		SomeClassWithNoDefaultConstructors foo = new SomeClassWithNoDefaultConstructors("foo", 123);
		byte[] bytes = serializer.serialize(foo);
		Object foo2 = serializer.deserialize(bytes, SomeClassWithNoDefaultConstructors.class);
		assertEquals(foo, foo2);
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

	@Test
	public void testPrimitiveSerialization() throws IOException {
		PojoSerializer serializer = new PojoSerializer();
		byte[] bytes;

		bytes = serializer.serialize(true);
		boolean b = (Boolean) serializer.deserialize(bytes, Boolean.class);
		assertEquals(true, b);
		b = (Boolean) serializer.deserialize(bytes, boolean.class);
		assertEquals(true, b);

		bytes = serializer.serialize(3.14159);
		double d = (Double) serializer.deserialize(bytes, double.class);
		d = (Double) serializer.deserialize(bytes, Double.class);
		assertEquals(3.14159, d, 0.00001);

	}
}
