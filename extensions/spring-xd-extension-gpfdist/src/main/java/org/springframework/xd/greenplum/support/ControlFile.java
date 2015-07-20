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

import java.util.ArrayList;
import java.util.List;

public class ControlFile {

	private Character gploadInputDelimiter;

	private String gploadOutputTable;

	private List<String> gploadOutputMatchColumns;

	private List<String> gploadOutputUpdateColumns;

	private String gploadOutputUpdateCondition;

	private OutputMode gploadOutputMode;

	private String database;

	private String user;

	private String password;

	private String host;

	private Integer port;

	private final List<String> gploadSqlBefore = new ArrayList<String>();

	private final List<String> gploadSqlAfter = new ArrayList<String>();

	public Character getGploadInputDelimiter() {
		return gploadInputDelimiter;
	}

	public void setGploadInputDelimiter(Character gploadInputDelimiter) {
		this.gploadInputDelimiter = gploadInputDelimiter;
	}

	public String getGploadOutputTable() {
		return gploadOutputTable;
	}

	public void setGploadOutputTable(String gploadOutputTable) {
		this.gploadOutputTable = gploadOutputTable;
	}

	public List<String> getGploadOutputMatchColumns() {
		return gploadOutputMatchColumns;
	}

	public void setGploadOutputMatchColumns(List<String> gploadOutputMatchColumns) {
		this.gploadOutputMatchColumns = gploadOutputMatchColumns;
	}

	public List<String> getGploadOutputUpdateColumns() {
		return gploadOutputUpdateColumns;
	}

	public void setGploadOutputUpdateColumns(List<String> gploadOutputUpdateColumns) {
		this.gploadOutputUpdateColumns = gploadOutputUpdateColumns;
	}

	public String getGploadOutputUpdateCondition() {
		return gploadOutputUpdateCondition;
	}

	public void setGploadOutputUpdateCondition(String gploadOutputUpdateCondition) {
		this.gploadOutputUpdateCondition = gploadOutputUpdateCondition;
	}

	public OutputMode getGploadOutputMode() {
		return gploadOutputMode;
	}

	public void setGploadOutputMode(OutputMode gploadOutputMode) {
		this.gploadOutputMode = gploadOutputMode;
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public List<String> getGploadSqlBefore() {
		return gploadSqlBefore;
	}

	public void addGploadSqlBefore(String gploadSqlBefore) {
		this.gploadSqlBefore.add(gploadSqlBefore);
	}

	public List<String> getGploadSqlAfter() {
		return gploadSqlAfter;
	}

	public void addGploadSqlAfter(String gploadSqlAfter) {
		this.gploadSqlAfter.add(gploadSqlAfter);
	}

	public enum OutputMode {
		INSERT, UPDATE, MERGE
	}

}
