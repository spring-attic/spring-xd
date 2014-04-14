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

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.xd.test.fixtures.JdbcSink;


/**
 * Runs a basic suite of JDBC tests on an XD Cluster instance.
 * 
 * @author Glenn Renfro
 */
public class JdbcTest extends AbstractIntegrationTest {


	JdbcSink jdbcSink = null;

	String tableName = null;

	@Before
	public void initialize() throws Exception {
		jdbcSink = sinks.jdbc();
		tableName = "acceptanceTEST12345";
		jdbcSink.tableName(tableName);
		try {
			jdbcSink.getJdbcTemplate().execute("drop table " + tableName);
		}
		catch (Exception ex) {
			// ignore just doing cleanup.
		}

	}

	/**
	 * * Verifies that data sent by the TCP sink that terminates with a CRLF works as expected.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testJDBCSink() throws Exception {
		String data = UUID.randomUUID().toString();
		jdbcSink.getJdbcTemplate().getDataSource();
		stream("dataSender", "trigger --payload='" + data + "'" + XD_DELIMETER + jdbcSink);

		waitForXD(2000);

		String query = String.format("SELECT payload FROM %s", tableName);
		assertEquals(
				data,
				jdbcSink.getJdbcTemplate().queryForObject(query, String.class));
	}

	@After
	public void cleanup() {
		if (jdbcSink == null) {
			return;
		}
		jdbcSink.getJdbcTemplate().execute("drop table " + tableName);
	}
}
