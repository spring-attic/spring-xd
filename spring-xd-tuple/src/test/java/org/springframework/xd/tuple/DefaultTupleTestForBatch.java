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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.xd.tuple.TupleBuilder.tuple;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.convert.ConversionFailedException;

/**
 * This is a port of the FieldSet tests from Spring Batch
 *
 */
public class DefaultTupleTestForBatch {
	
	Tuple tuple;
	
	List<Object> values;
	
	List<String> names;
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	
	@Before
	public void setUp() throws Exception {

		String[] tokens = new String[] { "TestString", "true", "C", "10", "-472", "354224", "543", "124.3", "424.3", "1,3245",
				null, "2007-10-12", "12-10-2007", "" };
		String[] nameArray = new String[] { "String", "Boolean", "Char", "Byte", "Short", "Integer", "Long", "Float", "Double",
				"BigDecimal", "Null", "Date", "DatePattern", "BlankInput" };

		names = Arrays.asList(nameArray);
		values = new ArrayList<Object>();
		for (String token : tokens) {
			values.add(token);
		}
		tuple = tuple().ofNamesAndValues(names, values);
		assertEquals(14, tuple.size());

	}
	
	@Test
	public void testNames() throws Exception {
		//MLP - tuples always have names, FieldSet in Spring Batch doesn't require a name.
		assertThat(tuple.getFieldCount(), is(tuple.getFieldNames().size()));
	}
	
	@Test
	public void testReadString() {
		assertThat(tuple.getString(0), is("TestString"));
		assertThat(tuple.getString("String"), is("TestString"));
	}

	@Test
	public void testReadChar() {
		assertThat(tuple.getChar(2), is('C'));
		assertThat(tuple.getChar("Char"), is('C'));
	}
	
	@Test
	public void testReadBooleanTrue() {
		assertThat(tuple.getBoolean(1), is(true));
		assertThat(tuple.getBoolean("Boolean"), is(true));
	}
	
	@Test
	public void testReadByte() {
		assertTrue(tuple.getByte(3) == 10);
		assertTrue(tuple.getByte("Byte") == 10);
	}
	
	@Test
	public void testReadShort() {
		assertTrue(tuple.getShort(4) == -472);
		assertTrue(tuple.getShort("Short") == -472);
	}
	
	@Test
	public void testReadIntegerAsFloat() {
		assertEquals(354224, tuple.getFloat(5), .001);
		assertEquals(354224, tuple.getFloat("Integer"), .001);
	}
	
	@Test
	public void testReadFloat() throws Exception {
		assertTrue(tuple.getFloat(7) == 124.3F);
		assertTrue(tuple.getFloat("Float") == 124.3F);
	}

	@Test
	public void testReadIntegerAsDouble() throws Exception {
		assertEquals(354224, tuple.getDouble(5), .001);
		assertEquals(354224, tuple.getDouble("Integer"), .001);
	}
	
	@Test
	public void testReadDouble() throws Exception {
		assertTrue(tuple.getDouble(8) == 424.3);
		assertTrue(tuple.getDouble("Double") == 424.3);
	}
	
	@Test
	public void testReadBigDecimal() throws Exception {
		BigDecimal bd = new BigDecimal("424.3");
		assertEquals(bd, tuple.getBigDecimal(8));
		assertEquals(bd, tuple.getBigDecimal("Double"));
	}
	
	@Test
	public void testReadBigBigDecimal() throws Exception {
		BigDecimal bd = new BigDecimal("12345678901234567890");
		Tuple tuple = TupleBuilder.tuple().of("bigd", "12345678901234567890");
		assertEquals(bd, tuple.getBigDecimal(0));
	}
	
	@Test
	public void testReadBigDecimalWithFormat() throws Exception {
		Tuple numberFormatTuple = TupleBuilder.tuple().setNumberFormatFromLocale(Locale.US).ofNamesAndValues(tuple.getFieldNames(), tuple.getValues());		
		BigDecimal bd = new BigDecimal("424.3");
		assertEquals(bd, numberFormatTuple.getBigDecimal(8));
	}
	
	@Test
	public void testReadBigDecimalWithEuroFormat() throws Exception {
		Tuple numberFormatTuple = TupleBuilder.tuple().setNumberFormatFromLocale(Locale.GERMANY).ofNamesAndValues(tuple.getFieldNames(), tuple.getValues());
		BigDecimal bd = new BigDecimal("1.3245");
		assertEquals(bd, numberFormatTuple.getBigDecimal(9));
	}
	
	@Test
	public void testReadNonExistentField() {
		String s = tuple.getString("something");
		assertThat(s, nullValue());
	}
	
	@Test
	public void testReadIndexOutOfRange() {
		try {
			tuple.getShort(-1);
			fail("field set returns value even index is out of range!");
		}
		catch (IndexOutOfBoundsException e) {
			assertTrue(true);
		}

		try {
			tuple.getShort(99);
			fail("field set returns value even index is out of range!");
		}
		catch (Exception e) {
			assertTrue(true);
		}
	}
	
	@Test
	public void testReadBooleanWithTrueValue() {
		assertTrue(tuple.getBoolean(1, "true"));
		assertFalse(tuple.getBoolean(1, "incorrect trueValue"));

		assertTrue(tuple.getBoolean("Boolean", "true"));
		assertFalse(tuple.getBoolean("Boolean", "incorrect trueValue"));
	}
	
	@Test
	public void testReadBooleanFalse() {
		Tuple t = TupleBuilder.tuple().of("foo", false);
		assertFalse(t.getBoolean(0));
	}
	
	@Test
	public void testReadCharException() {
		try {
			tuple.getChar(1);
			fail("the value read was not a character, exception expected");
		}
		catch (IllegalArgumentException expected) {
			assertTrue(true);
		}

		try {
			tuple.getChar("Boolean");
			fail("the value read was not a character, exception expected");
		}
		catch (IllegalArgumentException expected) {
			assertTrue(true);
		}
	}


	@Test
	public void testReadInt() throws Exception {		
		assertThat(354224, equalTo(tuple.getInt(5)));
		assertThat(354224, equalTo(tuple.getInt("Integer")));
	}
	
	@Test
	public void testReadIntWithSeparator() {
		Tuple t = TupleBuilder.tuple().of("foo", "354,224");
		assertThat(354224, equalTo(t.getInt(0)));
	}
	
	@Test
	public void testReadIntWithSeparatorAndFormat() throws Exception {
		Tuple t = TupleBuilder.tuple().setNumberFormatFromLocale(Locale.GERMAN).of("foo", "354.224");		
		assertThat(354224, equalTo(t.getInt(0)));
	}
	
	@Test
	@Ignore("Difference in behavior due to returning Wrapper types that may be null.")
	public void testReadBlankInt() {

		// Trying to parse a blank field as an integer, but without a default
		// value should throw a NumberFormatException
		try {
			tuple.getInt(13);
			fail();
		}
		catch (NumberFormatException ex) {
			// expected
		}

		try {
			tuple.getInt("BlankInput");
			fail();
		}
		catch (NumberFormatException ex) {
			// expected
		}

	}
	
	@Test
	public void testReadLong() throws Exception {
		assertThat(543L, equalTo(tuple.getLong(6)));
		assertThat(543L, equalTo(tuple.getLong("Long")));
	}
	
	@Test
	public void testReadLongWithPadding() throws Exception {		
		Tuple t = TupleBuilder.tuple().of("foo", "000009");
		assertThat(9L, equalTo(t.getLong(0)));
	}
	
	@Test
	@Ignore("Ignored until determine how to handle null value with wrapper types as well as default values")
	public void testReadIntWithNullValue() {	
	}
	
	@Test
	@Ignore("Ignored until determine how to handle null value with wrapper types as well as default values")
	public void testReadIntWithDefaultAndNotNull() {
	}
	
	@Test
	
	public void testReadBigDecimalInvalid() {
		int index = 0;

		try {
			tuple.getBigDecimal(index);
			fail("field value is not a number, exception expected");
		}
		//TODO - in batch this used to be IllegalArgumentException (which is the nested exception type now)
		catch (ConversionFailedException e) {
			assertTrue(e.getMessage().indexOf("TestString") > 0);
		}

	}
	
	@Test
	public void testReadBigDecimalByNameInvalid() throws Exception {
		try {
			tuple.getBigDecimal("String");
			fail("field value is not a number, exception expected");
		}
		catch (ConversionFailedException e) {
			assertTrue(e.getMessage().indexOf("TestString") > 0);
			//TODO - in batch this is part of the message, indicating what the name of the field is...
			//assertTrue(e.getMessage().indexOf("name: [String]") > 0);
		}
	}
	

	@Test
	public void testReadDate() throws Exception {
		assertNotNull(tuple.getDate(11));
		assertNotNull(tuple.getDate("Date"));
	}
	
	@Test
	@Ignore("Ignored until determine how to handle default values")
	public void testReadDateWithDefault() {	
	}
	
	@Test
	public void testReadDateWithFormat() throws Exception {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		Tuple t = TupleBuilder.tuple().setDateFormat(dateFormat).of("foo", "13/01/1999");
		assertEquals(dateFormat.parse("13/01/1999"), t.getDate(0));		
	}
	
	@Test
	public void testReadDateInvalid() throws Exception {
		try {
			tuple.getDate(0);
			fail("field value is not a date, exception expected");
		}
		catch (ConversionFailedException e) {
			assertTrue(e.getMessage().indexOf("TestString") > 0);
		}

	}
	
	@Test
	public void testReadDateInvalidByName() throws Exception {

		try {
			tuple.getDate("String");
			fail("field value is not a date, exception expected");
		}
		catch (ConversionFailedException e) {
			assertTrue(e.getMessage().indexOf("TestString") > 0);
			//TODO - in batch this is part of the message, indicating what the name of the field is...
			//assertTrue(e.getMessage().indexOf("name: [String]") > 0);
		}

	}
	
	@Test
	public void testReadDateInvalidWithPattern() throws Exception {

		try {
			tuple.getDate(0, "dd-MM-yyyy");
			fail("field value is not a date, exception expected");
		}
		catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().indexOf("dd-MM-yyyy") > 0);
		}
	}
	
	@Test
	@Ignore("Ignored until determine how to handle default values")
	public void testReadDateWithPatternAndDefault() {
		
	}
	
	@Test
	public void testStrictReadDateWithPattern() throws Exception {

		Tuple t = tuple().of("foo", "50-2-13");
		//fieldSet = new DefaultFieldSet(new String[] {"50-2-13"});
		try {
			t.getDate(0, "dd-MM-yyyy");
			fail("field value is not a valid date for strict parser, exception expected");
		}
		catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue("Message did not contain: " + message, message.indexOf("dd-MM-yyyy") > 0);
		}
	}
	
	@Test
	public void testStrictReadDateWithPatternAndStrangeDate() throws Exception {

		Tuple t = tuple().of("foo","5550212");
		try {
			System.err.println(t.getDate(0, "yyyyMMdd"));
			fail("field value is not a valid date for strict parser, exception expected");
		}
		catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue("Message did not contain: " + message, message.indexOf("yyyyMMdd") > 0);
		}
	}
	
	@Test
	public void testReadDateByNameInvalidWithPattern() throws Exception {

		try {
			tuple.getDate("String", "dd-MM-yyyy");
			fail("field value is not a date, exception expected");
		}
		catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().indexOf("dd-MM-yyyy") > 0);
			assertTrue(e.getMessage().indexOf("String") > 0);
		}
	}
	
	//TODO look at differences (names with padding...) with getProperties and toMap in DefaultTuple (but not exposed on interface)
	
}
