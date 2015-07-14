/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.greenplum.support;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

public class DefaultLoadService implements LoadService {

	private final JdbcTemplate jdbcTemplate;

	public DefaultLoadService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		Assert.notNull(jdbcTemplate, "JdbcTemplate must be set");
	}

	@Override
	public void load(LoadConfiguration loadConfiguration) {
		load(loadConfiguration, null);
	}

	@Override
	public void load(LoadConfiguration loadConfiguration, RuntimeContext context) {
		String prefix = UUID.randomUUID().toString().replaceAll("-", "_");

		// setup jdbc operations
		JdbcCommands operations = new JdbcCommands(jdbcTemplate);

		String sqlCreateTable = SqlUtils.createExternalReadableTable(loadConfiguration, prefix,
				context != null ? context.getLocations() : null);
		String sqlDropTable = SqlUtils.dropExternalReadableTable(loadConfiguration, prefix);
		String sqlInsert = SqlUtils.load(loadConfiguration, prefix);

		operations.setPrepareSql(sqlCreateTable);
		operations.setCleanSql(sqlDropTable);
		operations.setRunSql(sqlInsert);

		operations.setBeforeSqls(loadConfiguration.getSqlBefore());
		operations.setAfterSqls(loadConfiguration.getSqlAfter());

		if (!operations.execute() && operations.getLastException() != null) {
			throw operations.getLastException();
		}
	}

}
