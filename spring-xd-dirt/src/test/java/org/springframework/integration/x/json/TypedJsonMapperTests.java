/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.x.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.joda.time.DateTime;
import org.junit.Test;

import org.springframework.integration.x.json.TypedJsonMapper;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author David Turanski
 * @author Gary Russell
 *
 */
public class TypedJsonMapperTests {

	private final TypedJsonMapper mapper = new TypedJsonMapper();

	@Test
	public void testRandomObjectSerialization() {
		Foo foo = new Foo("hello");
		byte[] bytes = mapper.toBytes(foo);
		Object obj = mapper.fromBytes(bytes);
		assertTrue(obj instanceof Foo);
		assertEquals("hello", ((Foo) obj).bar);
	}

	@Test
	public void testDateSerialization() {
		Date d = new Date();
		byte[] bytes = mapper.toBytes(d);
		Date obj = (Date) mapper.fromBytes(bytes);
		assertEquals(d, obj);
	}

	@Test
	public void testDateTimeSerialization() {
		DateTime d = new DateTime();
		byte[] bytes = mapper.toBytes(d);
		DateTime obj = (DateTime) mapper.fromBytes(bytes);
		assertEquals(d, obj);
	}

	@Test
	public void testStringSerialization() {
		String s = new String("hello");
		byte[] bytes = mapper.toBytes(s);
		Object obj = mapper.fromBytes(bytes);
		assertEquals(s, obj);
	}

	@Test
	public void testLongSerialization() {
		byte[] bytes = mapper.toBytes(100L);
		long obj = (Long) mapper.fromBytes(bytes);
		assertEquals(100, obj);
	}

	@Test
	public void testFloatSerialization() {
		byte[] bytes = mapper.toBytes(99.9f);
		double obj = (Double) mapper.fromBytes(bytes);
		assertEquals(99.9, obj, 0.1);
	}

	@Test
	public void testBooleanSerialization() {
		byte[] bytes = mapper.toBytes(true);
		boolean obj = (Boolean) mapper.fromBytes(bytes);
		assertTrue(obj);
	}

	@Test
	public void testMapSerialization() {
		Map<String,String> map = new HashMap<String,String>();
		map.put("foo","bar");
		byte[] bytes = mapper.toBytes(map);
		Map<?,?> obj = (Map<?,?>)mapper.fromBytes(bytes);
		assertEquals("bar",obj.get("foo"));
	}

	@Test
	public void testListSerialization() {
		List<String> list = new LinkedList<String>();
		list.add("foo");
		byte[] bytes = mapper.toBytes(list);
		List<?> obj = (List<?>)mapper.fromBytes(bytes);
		assertEquals("foo",obj.get(0));
	}

	@Test
	public void testSetSerialization() {
		Set<String> set = new TreeSet<String>();
		set.add("foo");
		byte[] bytes = mapper.toBytes(set);
		Set<?> obj = (Set<?>)mapper.fromBytes(bytes);
		assertEquals("foo",obj.iterator().next());
	}

	@Test
	public void testTupleSerialization() {
		Tuple t = TupleBuilder.tuple().of("foo", "bar");
		byte[] bytes = mapper.toBytes(t);

		Tuple obj = (Tuple) mapper.fromBytes(bytes);
		assertEquals("bar", obj.getString("foo"));
	}

	public static class Foo {
		@JsonCreator
		public Foo(@JsonProperty("bar") String val) {
			bar = val;
		}

		public String bar;
	}

}
