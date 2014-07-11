/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.tuple.batch;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.xd.tuple.Tuple;

public class TupleFieldSetMapperTests {

	private TupleFieldSetMapper mapper;

	private static DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

	@Before
	public void setUp() throws Exception {
		mapper = new TupleFieldSetMapper();
	}

	@Test
	public void testVanillaTupleCreation() throws Exception {
		FieldSet fieldSet = new DefaultFieldSet(new String[] { "This is some dummy string", "true", "C" },
				new String[] { "varString", "varBoolean", "varChar" });
		Tuple result = mapper.mapFieldSet(fieldSet);
		assertEquals("This is some dummy string", result.getString(0));
		assertEquals(true, result.getBoolean(1));
		assertEquals('C', result.getChar(2));
	}

	@Test
	@SuppressWarnings("serial")
	public void testValueConversionByName() throws Exception {
		mapper.setTypes(new HashMap<String, FieldSetType>() {

			{
				put("field1", FieldSetType.BIG_DECIMAL);
				put("field2", FieldSetType.BOOLEAN);
				put("field3", FieldSetType.BYTE);
				put("field4", FieldSetType.CHAR);
				put("field5", FieldSetType.DATE);
				put("field6", FieldSetType.DOUBLE);
				put("field7", FieldSetType.FLOAT);
				put("field8", FieldSetType.INT);
				put("field9", FieldSetType.LONG);
				put("field10", FieldSetType.SHORT);
				put("field11", FieldSetType.STRING);
			}
		});
		FieldSet fieldSet = new DefaultFieldSet(new String[] { "19.95", "true", "1", "M", "1977-10-22", "2.22", "9.99",
			"5", "9", "10", "some dummy string", "2007-06-23" },
				new String[] { "field1", "field2", "field3", "field4", "field5", "field6", "field7", "field8",
					"field9", "field10", "field11", "field12" });
		Tuple result = mapper.mapFieldSet(fieldSet);
		assertEquals(new BigDecimal("19.95"), result.getValue("field1"));
		assertEquals(true, result.getValue("field2"));
		assertEquals((byte) 1, result.getValue("field3"));
		assertEquals('M', result.getValue("field4"));
		assertEquals(formatter.parse("1977-10-22"), result.getValue("field5"));
		assertEquals(2.22, result.getValue("field6"));
		assertEquals(9.99f, result.getValue("field7"));
		assertEquals(5, result.getValue("field8"));
		assertEquals(9l, result.getValue("field9"));
		assertEquals((short) 10, result.getValue("field10"));
		assertEquals("some dummy string", result.getValue("field11"));
		assertEquals("2007-06-23", result.getValue("field12"));
	}


	@Test
	public void testCustomDateFormatting() throws Exception {
		SimpleDateFormat customFormatter = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
		mapper.setDateFormat(customFormatter);

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "10/22/1977 01:25 AM" }, new String[] { "dateField" });

		Tuple result = mapper.mapFieldSet(fieldSet);

		assertEquals(customFormatter.parse("10/22/1977 01:25 AM"), result.getDate(0));
	}
}
