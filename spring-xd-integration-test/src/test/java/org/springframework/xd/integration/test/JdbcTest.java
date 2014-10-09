/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.integration.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.dao.DataAccessException;
import org.springframework.xd.test.fixtures.JdbcSink;


/**
 * Runs a basic suite of JDBC tests on an XD Cluster instance.
 *
 * @author Glenn Renfro
 */
public class JdbcTest extends AbstractIntegrationTest {


	private JdbcSink jdbcSink;

	private String tableName;

	/**
	 * Removes the table created from a previous test.
	 */
	@Before
	public void initialize() {
		jdbcSink = sinks.jdbc();
		tableName = "acceptanceTEST12345";
		jdbcSink.tableName(tableName);
		cleanup();
	}

	/**
	 * * Verifies that Jdbc sink has written the test data to the table.
	 *
	 */
	@Test
	public void testJDBCSink() {
		String data = UUID.randomUUID().toString();
		stream("dataSender", "trigger --payload='" + data + "'" + XD_DELIMITER + jdbcSink);

		String query = String.format("SELECT payload FROM %s", tableName);
		waitForTablePopulation(query, jdbcSink.getJdbcTemplate(), 1);
		assertEquals(
				data,
				jdbcSink.getJdbcTemplate().queryForObject(query, String.class));
	}
	/**
	 * * Verifies that when the initializeDatabase option is set to false that the tables are not overwritten.
	 *
	 */
	@Test
	public void testJDBCNoInitializeSink() {
		String data = UUID.randomUUID().toString();
		String jdbcStreamName = UUID.randomUUID().toString();
		stream("jdbcInit"+jdbcStreamName, "trigger --payload='" + data + "'" + XD_DELIMITER + jdbcSink);
		String query = String.format("SELECT payload FROM %s", tableName);
		waitForTablePopulation(query, jdbcSink.getJdbcTemplate(), 1);

		String data2 = UUID.randomUUID().toString();
		stream("jdbcInitNoInit"+jdbcStreamName, "trigger --payload='" + data2 + "'" + XD_DELIMITER + jdbcSink.initializeDB(false));
		waitForTablePopulation(query, jdbcSink.getJdbcTemplate(), 2);

		List<Map<String,Object>> resultList = jdbcSink.getJdbcTemplate().queryForList(query);
		assertEquals(2,resultList.size());
		int dataCount = 0;
		for(Map<String,Object> map:resultList){
			if(map.get("payload").equals(data2)){
				dataCount++;
			}
		}
		assertEquals("There should be one "+data2+" in the DB.",1, dataCount);


	}
	/**
	 * Being a good steward of the database remove the result table from the database.
	 */
	@After
	public void cleanup() {
		if (jdbcSink == null) {
			return;
		}
		try {
			jdbcSink.getJdbcTemplate().execute("drop table " + tableName);
		}
		catch (DataAccessException daException) {
			// This exception is thrown if the table is not present. In this case that is ok.
		}
	}
}
