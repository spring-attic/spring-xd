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
