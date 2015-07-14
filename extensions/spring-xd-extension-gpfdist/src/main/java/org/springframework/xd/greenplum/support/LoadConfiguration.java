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

import java.util.List;

public class LoadConfiguration {

	private String table;

	private String columns;

	private ReadableTable externalTable;

	private Mode mode;

	private List<String> matchColumns;

	private List<String> updateColumns;

	private String updateCondition;

	private List<String> sqlBefore;

	private List<String> sqlAfter;

	public LoadConfiguration() {
		super();
	}

	public LoadConfiguration(String table, String columns, ReadableTable externalTable, Mode mode,
			List<String> matchColumns, List<String> updateColumns, String updateCondition) {
		this.table = table;
		this.columns = columns;
		this.externalTable = externalTable;
		this.mode = mode;
		this.matchColumns = matchColumns;
		this.updateColumns = updateColumns;
		this.updateCondition = updateCondition;
	}

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}

	public String getColumns() {
		return columns;
	}

	public void setColumns(String columns) {
		this.columns = columns;
	}

	public ReadableTable getExternalTable() {
		return externalTable;
	}

	public void setExternalTable(ReadableTable externalTable) {
		this.externalTable = externalTable;
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public List<String> getMatchColumns() {
		return matchColumns;
	}

	public void setMatchColumns(List<String> matchColumns) {
		this.matchColumns = matchColumns;
	}

	public List<String> getUpdateColumns() {
		return updateColumns;
	}

	public void setUpdateColumns(List<String> updateColumns) {
		this.updateColumns = updateColumns;
	}

	public String getUpdateCondition() {
		return updateCondition;
	}

	public void setUpdateCondition(String updateCondition) {
		this.updateCondition = updateCondition;
	}

	public List<String> getSqlBefore() {
		return sqlBefore;
	}

	public void setSqlBefore(List<String> sqlBefore) {
		this.sqlBefore = sqlBefore;
	}

	public List<String> getSqlAfter() {
		return sqlAfter;
	}

	public void setSqlAfter(List<String> sqlAfter) {
		this.sqlAfter = sqlAfter;
	}

}
