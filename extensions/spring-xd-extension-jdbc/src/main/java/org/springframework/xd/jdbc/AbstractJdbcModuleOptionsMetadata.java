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
public abstract class AbstractJdbcModuleOptionsMetadata {

	protected String driverClass;

	protected Boolean initializeDatabase;

	protected String initializerScript;

	protected String password;

	protected String tableName;

	protected String url;

	protected String username;

	protected String configProperties;


	@ModuleOption("the JDBC driver to use")
	public void setDriverClass(String driverClass) {
		this.driverClass = driverClass;
	}

	@ModuleOption("whether the database initialization script should be run")
	public void setInitializeDatabase(Boolean initializeDatabase) {
		this.initializeDatabase = initializeDatabase;
	}

	@ModuleOption("the JDBC password")
	public void setPassword(String password) {
		this.password = password;
	}

	@ModuleOption("the database table to which the data will be written")
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	@ModuleOption("the JDBC URL for the database")
	public void setUrl(String url) {
		this.url = url;
	}

	@ModuleOption("the JDBC username")
	public void setUsername(String username) {
		this.username = username;
	}

	@ModuleOption("the name of the SQL script (in /config) to run if 'initializeDatabase' is set")
	public void setInitializerScript(String initializerScript) {
		this.initializerScript = initializerScript;
	}

	@ModuleOption("the name of the properties file (in /config) used to override database settings")
	public void setConfigProperties(String configProperties) {
		this.configProperties = configProperties;
	}

	public String getDriverClass() {
		return driverClass;
	}

	public Boolean getInitializeDatabase() {
		return initializeDatabase;
	}

	public String getPassword() {
		return password;
	}

	public String getTableName() {
		return tableName;
	}

	public String getUrl() {
		return url;
	}

	public String getUsername() {
		return username;
	}

	public String getInitializerScript() {
		return initializerScript;
	}

	public String getConfigProperties() {
		return configProperties;
	}

}
