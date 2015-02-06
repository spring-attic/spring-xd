/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.test.fixtures;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Represents the {@code jdbc} source module. Embeds a datasource to which
 * data can be added, so that it gets picked up by the module.
 *
 * @author Eric Bottard
 */
public class JdbcSource extends AbstractModuleFixture<JdbcSink> implements Disposable {

	private final DataSource dataSource;
	private final JdbcTemplate jdbcTemplate;
	private String update;
	private String query;
	private int fixedDelay = 5;

	public JdbcSource(DataSource dataSource) {

		this.dataSource = dataSource;
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	@Override
	protected String toDSL() {
		String result = null;
		Connection connection = null;
		try {
			connection = dataSource.getConnection();
			result = String.format("jdbc --query='%s' --url=%s --fixedDelay=%d", shellEscape(query), connection.getMetaData().getURL(), fixedDelay);
		}
		catch (SQLException e) {
			throw new IllegalStateException(e);
		}
		finally {
			if (connection != null) {
				try {
					connection.close();
				}
				catch (SQLException e) {
				}
			}
		}
		if (update != null) {
			result += String.format(" --update='%s'", shellEscape(update));
		}
		return result;

	}

	/**
	 * Escapes quotes for the XD shell.
	 */
	private String shellEscape(String sql) {
		return sql.replace("'", "''");
	}

	@Override
	public void cleanup() {
		jdbcTemplate.execute("SHUTDOWN");
	}

	public JdbcSource update(String update) {
		this.update = update;
		return this;
	}

	public JdbcSource query(String query) {
		this.query = query;
		return this;
	}

	public JdbcSource fixedDelay(int delay) {
		this.fixedDelay = delay;
		return this;
	}
}
