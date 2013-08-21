
package org.springframework.xd.tuple.batch;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.xd.tuple.Tuple;

public class TupleRowMapperTests {

	private RowMapper<Tuple> mapper;

	@Mock
	private ResultSet rs;

	@Mock
	private ResultSetMetaData metaData;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		mapper = new TupleRowMapper();
	}

	@Test
	public void test() throws Exception {
		when(metaData.getColumnCount()).thenReturn(3);
		when(metaData.getColumnName(1)).thenReturn("first");
		when(rs.getObject(1)).thenReturn("string");
		when(metaData.getColumnName(2)).thenReturn("second");
		when(rs.getObject(2)).thenReturn(5);
		when(metaData.getColumnName(3)).thenReturn("third");
		when(rs.getObject(3)).thenReturn(true);

		when(rs.getMetaData()).thenReturn(metaData);

		Tuple result = mapper.mapRow(rs, 0);

		assertEquals("string", result.getValue("first"));
		assertEquals(5, result.getValue("second"));
		assertEquals(true, result.getValue("third"));
	}
}
