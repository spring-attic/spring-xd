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

package org.springframework.xd.shell.command.fixtures;

import java.sql.Driver;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;


/**
 * Represents the {@code jdbc} sink module. Maintains an in memory relational database and exposes a
 * {@link JdbcTemplate} so that assertions can be made against it.
 * 
 * @author Florent Biville
 */
public class JdbcSink extends AbstractModuleFixture implements Disposable {

	private JdbcTemplate jdbcTemplate;

	private String jdbcUrl;

	private String tableName;

	private String columns;

	private String dbname = "foo";

	private String driver = "org.hsqldb.jdbc.JDBCDriver";

	private String username;

	private String url = "jdbc:hsqldb:mem:%s";

	private String password;

	private String configFileName;

	public JdbcSink dbname(String dbname) {
		this.dbname = dbname;
		return this;
	}

	public JdbcSink start() throws Exception {
		initDatasource();

		return this;
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	@Override
	protected String toDSL() {
		String dsl = "jdbc --initializeDatabase=true --url=" + jdbcUrl;
		if (tableName != null) {
			dsl += " --tableName=" + tableName;
		}
		if (columns != null) {
			dsl += " --columns=" + columns;
		}
		if (configFileName != null) {
			dsl += " --configProperties=" + configFileName;
		}
		return dsl;
	}

	private void initDatasource() throws Exception {

		jdbcUrl = String.format(url, dbname);
		SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
		@SuppressWarnings("unchecked")
		Class<? extends Driver> classz = (Class<? extends Driver>) Class.forName(driver);
		dataSource.setDriverClass(classz);
		dataSource.setUrl(jdbcUrl);
		if (password != null) {
			dataSource.setPassword(password);
		}
		if (username != null) {
			dataSource.setUsername(username);
		}
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public void cleanup() {
		jdbcTemplate.execute("SHUTDOWN");
	}

	public JdbcSink tableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	public JdbcSink columns(String columns) {
		this.columns = columns;
		return this;
	}

	public JdbcSink password(String password) {
		this.password = password;
		return this;
	}

	public JdbcSink url(String url) {
		this.url = url;
		return this;
	}

	public JdbcSink username(String username) {
		this.username = username;
		return this;
	}

	public JdbcSink driver(String driver) {
		this.driver = driver;
		return this;
	}

	public JdbcSink database(String database) {
		this.dbname = database;
		return this;
	}

	public JdbcSink configFile(String configFileName) {
		this.configFileName = configFileName;
		return this;
	}

}
