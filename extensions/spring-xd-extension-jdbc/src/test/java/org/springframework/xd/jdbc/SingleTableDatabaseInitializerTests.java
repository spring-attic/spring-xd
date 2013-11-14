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

package org.springframework.xd.jdbc;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;

import org.junit.After;
import org.junit.Test;

import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class SingleTableDatabaseInitializerTests {

	private final EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();

	private final EmbeddedDatabase db = builder.build();

	private final SingleTableDatabaseInitializer databaseInitializer = new SingleTableDatabaseInitializer();

	private final ClassRelativeResourceLoader resourceLoader = new ClassRelativeResourceLoader(getClass());

	private final JdbcTemplate jdbcTemplate = new JdbcTemplate(db);

	@After
	public void shutDown() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
			TransactionSynchronizationManager.unbindResource(db);
		}
		db.shutdown();
	}

	@Test
	public void testBuildWithPlaceholders() throws Exception {
		databaseInitializer.addScript(resourceLoader.getResource("db-schema-with-placeholder.sql"));
		databaseInitializer.setTableName("DEMO");
		databaseInitializer.setColumnNames(new String[] {"blah0", "blah1", "blah2"});
		databaseInitializer.setIgnoreFailedDrops(true);
		databaseInitializer.afterPropertiesSet();
		Connection connection = db.getConnection();
		try {
			databaseInitializer.populate(connection);
		}
		finally {
			connection.close();
		}

		assertTestDatabaseCreated("DEMO");
		// Check a select with column names works
		jdbcTemplate.execute("select blah0, blah1, blah2 from demo");
	}

	@Test
	public void testBuildWithoutPlaceholders() throws Exception {
		databaseInitializer.addScript(resourceLoader.getResource("db-schema-without-placeholder.sql"));
		databaseInitializer.setIgnoreFailedDrops(true);
		databaseInitializer.afterPropertiesSet();
		Connection connection = db.getConnection();
		try {
			databaseInitializer.populate(connection);
		}
		finally {
			connection.close();
		}

		assertTestDatabaseCreated("T_TEST");
	}

	private void assertTestDatabaseCreated(String name) {
		assertEquals(0, jdbcTemplate.queryForObject("select count(*) from " + name, Number.class).intValue());
	}

}
