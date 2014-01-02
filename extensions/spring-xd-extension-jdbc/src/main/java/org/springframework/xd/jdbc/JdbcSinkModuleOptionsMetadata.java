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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.SinkModuleOptionsMetadataSupport;


/**
 * Captures module options for the "jdbc" sink module.
 * 
 * @author Eric Bottard
 */
@PropertySource(value = "${xd.config.home}/${configProperties:jdbc}.properties", ignoreResourceNotFound = true)
public class JdbcSinkModuleOptionsMetadata extends SinkModuleOptionsMetadataSupport {

	private String tableName;

	private String columns;

	@Value("${initializeDatabase}")
	private boolean initializeDatabase;

	@Value("${initializerScript}")
	private String initializerScript = "init_db.sql";

	@Value("${driverClass}")
	private String driverClass;

	@Value("${url}")
	private String url;

	@Value("${username}")
	private String username;

	@Value("${password}")
	private String password;


	public String getTableName() {
		return tableName;
	}

	@ModuleOption("the database table to which the data will be written")
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}


	public String getColumns() {
		return columns;
	}

	@ModuleOption("the database columns to map the data to")
	public void setColumns(String columns) {
		this.columns = columns;
	}


	public boolean isInitializeDatabase() {
		return initializeDatabase;
	}

	@ModuleOption("whether the database initialization script should be run")
	public void setInitializeDatabase(boolean initializeDatabase) {
		this.initializeDatabase = initializeDatabase;
	}


	public String getInitializerScript() {
		return initializerScript;
	}

	@ModuleOption("the name of the SQL script (in /config) to run if 'initializeDatabase' is set")
	public void setInitializerScript(String initializerScript) {
		this.initializerScript = initializerScript;
	}


	public String getDriverClass() {
		return driverClass;
	}

	@ModuleOption("the JDBC driver to use")
	public void setDriverClass(String driverClass) {
		this.driverClass = driverClass;
	}


	public String getUrl() {
		return url;
	}

	@ModuleOption("the JDBC URL for the database")
	public void setUrl(String url) {
		this.url = url;
	}


	public String getUsername() {
		return username;
	}

	@ModuleOption("the JDBC username")
	public void setUsername(String username) {
		this.username = username;
	}


	public String getPassword() {
		return password;
	}

	@ModuleOption("the JDBC password")
	public void setPassword(String password) {
		this.password = password;
	}


}
