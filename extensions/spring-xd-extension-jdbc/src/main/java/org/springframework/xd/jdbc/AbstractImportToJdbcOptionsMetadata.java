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

import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Factors out options that are common to modules/jobs that import into a db table.
 * 
 * @author Eric Bottard
 */
public abstract class AbstractImportToJdbcOptionsMetadata {

	protected boolean initializeDatabase;

	protected String initializerScript;

	protected String tableName;


	@ModuleOption("whether the database initialization script should be run")
	public void setInitializeDatabase(boolean initializeDatabase) {
		this.initializeDatabase = initializeDatabase;
	}

	@ModuleOption("the database table to which the data will be written")
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	@ModuleOption("the name of the SQL script (in /config) to run if 'initializeDatabase' is set")
	public void setInitializerScript(String initializerScript) {
		this.initializerScript = initializerScript;
	}

	public boolean getInitializeDatabase() {
		return initializeDatabase;
	}

	public String getTableName() {
		return tableName;
	}

	public String getInitializerScript() {
		return initializerScript;
	}

}
