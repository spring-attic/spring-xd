package org.springframework.xd.tuple;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

public class SpringToDateConverterTests {

	@Test
	public void testCtor() throws ParseException {
		StringToDateConverter converter = new StringToDateConverter();
		Date d = converter.convert("2013-05-02");
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		assertEquals(dateFormat.parse("2013-05-02"), d);	
	}

}
