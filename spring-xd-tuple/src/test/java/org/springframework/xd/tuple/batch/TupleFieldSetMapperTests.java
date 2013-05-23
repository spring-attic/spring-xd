package org.springframework.xd.tuple.batch;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.xd.tuple.Tuple;

public class TupleFieldSetMapperTests {

	private TupleFieldSetMapper mapper;

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
		mapper.setTypes(new HashMap<String, FieldSetType>() {{
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
		}});
		FieldSet fieldSet = new DefaultFieldSet(new String[] { "19.95", "true", "1", "M", "1977-10-22", "2.22", "9.99", "5", "9", "10", "some dummy string", "2007-06-23" },
				new String[] { "field1", "field2", "field3", "field4", "field5", "field6", "field7", "field8", "field9", "field10", "field11", "field12" });
		Tuple result = mapper.mapFieldSet(fieldSet);
		assertEquals(new BigDecimal("19.95"), result.getValue("field1"));
		assertEquals(true, result.getValue("field2"));
		assertEquals((byte) 1, result.getValue("field3"));
		assertEquals('M', result.getValue("field4"));
		assertEquals(new Date(246344400000l), result.getValue("field5"));
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
		mapper.setDateFormat(new SimpleDateFormat("MM/dd/yyyy hh:mm a"));

		FieldSet fieldSet = new DefaultFieldSet(new String[] {"10/22/1977 01:25 AM"}, new String [] {"dateField"});

		Tuple result = mapper.mapFieldSet(fieldSet);

		assertEquals(new Date(246349500000l), result.getDate(0));
	}
}
