/*
 * Copyright 2013 the original author or authors.
 *
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
package org.springframework.xd.tuple;

import static org.junit.Assert.*;
import static org.springframework.xd.tuple.TupleBuilder.tuple;
import static org.hamcrest.Matchers.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

public class DefaultTupleTests {


	@Test(expected=IllegalArgumentException.class)
	public void nullForNameArray() {
		List<Object> values = new ArrayList<Object>();
		values.add("bar");
		tuple().ofNamesAndValues(null, values);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void nullForValueArray() {
		List<String> names = new ArrayList<String>();
		names.add("foo");
		tuple().ofNamesAndValues(names, null);
	}
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Test
	public void notEqualNumberOfNamesAndValues() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Field names must be same length as values: names=[foo, oof], values=[bar]");
		List<String> names = new ArrayList<String>();
		names.add("foo");
		names.add("oof");
		List<Object> values = new ArrayList<Object>();
		values.add("bar");
		tuple().ofNamesAndValues(names, values);
	}
	
	@Test
	public void accessNonExistentEntry() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Cannot access field [does-not-exist] from [foo]");
		Tuple tuple = TupleBuilder.tuple().of("foo", "bar");
		tuple.getValue("does-not-exist");
	}
	
	@Test
	public void singleEntry() {
		Tuple tuple = TupleBuilder.tuple().of("foo", "bar");
		assertThat(tuple.size(), equalTo(1));
		List<String> names = tuple.getFieldNames();
		assertThat(names.get(0), equalTo("foo"));
		assertThat((String)tuple.getValue("foo"), equalTo("bar"));
		assertThat(tuple.hasFieldName("foo"), equalTo(true));
		//assertThat(tuple.get("foo").asString(), equalTo("bar"));
	}
	
	
	
	@Test
	public void testId() {
		Tuple tuple1 = TupleBuilder.tuple().of("foo", "bar");
		assertThat(tuple1.getId(), notNullValue());
		Tuple tuple2 = TupleBuilder.tuple().of("foo", "bar");
		assertNotSame(tuple1.getId(), tuple2.getId());
	}
	
	@Test
	public void testTimestamp() throws Exception {
		Tuple tuple1 = TupleBuilder.tuple().of("foo", "bar");
		assertThat(tuple1.getTimestamp(), notNullValue());
		Thread.sleep(100L);
		Tuple tuple2 = TupleBuilder.tuple().of("foo", "bar");
		assertNotSame(tuple1.getTimestamp(), tuple2.getTimestamp());
	}
	

	@Test 
	public void twoEntries() {
		Tuple tuple = TupleBuilder.tuple().of("up", "down", "charm", "strange");
		assertTwoEntries(tuple);
	}

	/**
	 * @param tuple
	 */
	private void assertTwoEntries(Tuple tuple) {
		assertThat(tuple.size(), equalTo(2));
		assertThat(tuple.getFieldNames().get(0), equalTo("up"));
		assertThat(tuple.getFieldNames().get(1), equalTo("charm"));
		assertThat((String)tuple.getValue("up"), equalTo("down"));
		assertThat((String)tuple.getValue("charm"), equalTo("strange"));
	}
	
	@Test 
	public void threeEntries() {
		Tuple tuple = TupleBuilder.tuple().of("up", 1, "charm", 2, "top", 3 );
		assertThat(tuple.size(), equalTo(3));
		assertThat(tuple.getFieldNames().get(0), equalTo("up"));
		assertThat(tuple.getFieldNames().get(1), equalTo("charm"));
		assertThat(tuple.getFieldNames().get(2), equalTo("top"));
		// access by name
		assertThat((Integer)tuple.getValue("up"), equalTo(1));
		assertThat((Integer)tuple.getValue("charm"), equalTo(2));
		assertThat((Integer)tuple.getValue("top"), equalTo(3));
		// access by position
		assertThat((Integer)tuple.getValue(0), equalTo(1));
		assertThat((Integer)tuple.getValue(1), equalTo(2));
		assertThat((Integer)tuple.getValue(2), equalTo(3));
		// access from separate collection
		List<Object> values = tuple.getValues();
		assertThat((Integer)values.get(0), equalTo(1));
		assertThat((Integer)values.get(1), equalTo(2));
		assertThat((Integer)values.get(2), equalTo(3));
	}
	
	@Test 
	public void fourEntries() {
		Tuple tuple = TupleBuilder.tuple().of("up", 1, "charm", 2, "top", 3, "e", 4);
		assertThat(tuple.size(), equalTo(4));
		assertThat(tuple.getFieldNames().get(0), equalTo("up"));
		assertThat(tuple.getFieldNames().get(1), equalTo("charm"));
		assertThat(tuple.getFieldNames().get(2), equalTo("top"));
		assertThat(tuple.getFieldNames().get(3), equalTo("e"));
		// access by name
		assertThat((Integer)tuple.getValue("up"), equalTo(1));
		assertThat((Integer)tuple.getValue("charm"), equalTo(2));
		assertThat((Integer)tuple.getValue("top"), equalTo(3));
		assertThat((Integer)tuple.getValue("e"), equalTo(4));
		// access by position
		assertThat((Integer)tuple.getValue(0), equalTo(1));
		assertThat((Integer)tuple.getValue(1), equalTo(2));
		assertThat((Integer)tuple.getValue(2), equalTo(3));
		assertThat((Integer)tuple.getValue(3), equalTo(4));
	}
	
	@Test
	public void getValue() {
		Tuple tuple = TupleBuilder.tuple().of("up", 1, "charm", 2.0, "top", true );
		assertThat( tuple.getValue(0, Integer.class), equalTo(1));
		assertThat( tuple.getValue(0, String.class), equalTo("1"));
		assertThat( tuple.getValue(1, Double.class), equalTo(2.0D));
		assertThat( tuple.getValue(2, Boolean.class), equalTo(true));
		assertThat( tuple.getValue(2, String.class), equalTo("true"));
		
		assertThat( tuple.getValue("up", Integer.class), equalTo(1));
		assertThat( tuple.getValue("up", String.class), equalTo("1"));
		assertThat( tuple.getValue("charm", Double.class), equalTo(2.0D));
		assertThat( tuple.getValue("top", Boolean.class), equalTo(true));
		assertThat( tuple.getValue("top", String.class), equalTo("true"));
	}
	
	@Test
	public void testPrimitiveGetters() {
		Tuple tuple = TupleBuilder.tuple().of("up", "down", "charm", 2.0, "top", true );
		assertThat( tuple.getBoolean("top"), equalTo(true));
		assertThat( tuple.getBoolean(2), equalTo(true));
		assertThat( tuple.getBoolean("up", "down"), equalTo(true));
	}
	
	@Test
	public void testToString() {
		Tuple tuple = TupleBuilder.tuple().put("up", "down")
				 .put("charm", "strange")
				 .build();
		
		String tupleString = tuple.toString();
		assertThat(tupleString, containsString("DefaultTuple [names=[up, charm], values=[down, strange],"));
		assertThat(tupleString, containsString("id="));
		assertThat(tupleString, containsString("timestamp="));		
	}
	
	@Test
	public void testPutApi() {
		TupleBuilder builder = TupleBuilder.tuple();
		Tuple tuple = builder.put("up", "down")
							 .put("charm", "strange")
							 .build();
		assertTwoEntries(tuple);
	}
	
	@Test
	public void testEqualsAndHashCodeSunnyDay() {
		Tuple tuple1 = TupleBuilder.tuple().of("up", 1, "charm", 2, "top", 3 );
		Tuple tuple2 = TupleBuilder.tuple().of("up", 1, "charm", 2, "top", 3 );
		assertThat(tuple1, equalTo(tuple2));	
		assertThat(tuple1.hashCode(), equalTo(tuple2.hashCode()));
		assertThat(tuple1, not(sameInstance(tuple2)));
	}
	
	@Test
	public void testEqualsAndHashFailureCases() {
		Tuple tuple1 = TupleBuilder.tuple().of("up", 1, "charm", 2, "top", 3 );
		Tuple tuple2 = TupleBuilder.tuple().of("up", 2, "charm", 3, "top", 4 );
		assertThat(tuple1, not(equalTo((tuple2))));			
		assertThat(tuple1.hashCode(), not(equalTo(tuple2.hashCode())));
		
		tuple1 = TupleBuilder.tuple().of("up", 1, "charm", 2, "top", 3 );
		tuple2 = TupleBuilder.tuple().of("top", 1, "charm", 2, "up", 3 );
		assertThat(tuple1, not(equalTo((tuple2))));			
		assertThat(tuple1.hashCode(), not(equalTo(tuple2.hashCode())));
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	public void testGetFieldTypes() {
		Tuple tuple = TupleBuilder.tuple().of("up", 1, "charm", 2, "top", 3 );
		Class[] expectedTypes = new Class[] { Integer.class, Integer.class, Integer.class };		
		assertThat(tuple.getFieldTypes(), equalTo(Arrays.asList(expectedTypes)));
		
		tuple = TupleBuilder.tuple().of("up", 1, "charm", 2.0f, "top", "bottom" );
		expectedTypes = new Class[] { Integer.class, Float.class, String.class };
		assertThat(tuple.getFieldTypes(), equalTo(Arrays.asList(expectedTypes)));
		
		tuple = TupleBuilder.tuple().of("up", 1, "charm", 2.0, "top", true );
		expectedTypes = new Class[] { Integer.class, Double.class, Boolean.class };
		assertThat(tuple.getFieldTypes(), equalTo(Arrays.asList(expectedTypes)));
		
		
	}
	
	@Test
	public void testGetString() {
		//test conversions of string, int, and float.
		Tuple tuple = TupleBuilder.tuple().of("up", "down", "charm", 2, "top", 2.0f );
		assertThat(tuple.getString("up"), equalTo("down"));
		assertThat(tuple.getString("charm"), equalTo("2"));
		assertThat(tuple.getString("top"), equalTo("2.0"));
		
	}
	
	@Test
	public void testGetStringThatFails() {
		Tuple tuple = TupleBuilder.tuple().of("up", "down", "charm", 2, "top", 2.0f, "black", Color.black );
		thrown.expect(ConverterNotFoundException.class);
		thrown.expectMessage("No converter found capable of converting from type java.awt.Color to type java.lang.String");
		assertThat(tuple.getString("black"), equalTo("omg"));
	}
	
	@Test
	public void testSelection() {
		Tuple tuple = tuple().put("red", "rot")
				.put("brown", "braun")
				.put("blue", "blau")
				.put("yellow", "gelb")
				.put("beige", "beige")
				.build();
		Tuple selectedTuple = tuple.select("?[key.startsWith('b')]");
		assertThat(selectedTuple.size(), equalTo(3));
		
		selectedTuple = tuple.select("^[key.startsWith('b')]");
		assertThat(selectedTuple.size(), equalTo(1));
		assertThat(selectedTuple.getFieldNames().get(0), equalTo("brown"));
		assertThat(selectedTuple.getString(0), equalTo("braun"));

		
		selectedTuple = tuple.select("?[value.length() < 4]");
		//TODO - assertions
		
	}
}
