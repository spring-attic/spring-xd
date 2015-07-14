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

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;
import org.springframework.xd.greenplum.support.ControlFile.OutputMode;

public class LoadConfigurationFactoryBean implements FactoryBean<LoadConfiguration>, InitializingBean {

	private ControlFile controlFile;

	private String table;

	private String columns;

	private ReadableTable externalTable;

	private Mode mode = Mode.INSERT;

	private List<String> matchColumns;

	private List<String> updateColumns;

	private String updateCondition;

	private List<String> sqlBefore;

	private List<String> sqlAfter;

	@Override
	public void afterPropertiesSet() throws Exception {
		if (controlFile != null) {
			if (controlFile.getGploadOutputMode() != null) {
				if (controlFile.getGploadOutputMode() == OutputMode.INSERT) {
					mode = Mode.INSERT;
				}
				else if (controlFile.getGploadOutputMode() == OutputMode.UPDATE) {
					mode = Mode.UPDATE;
				}
			}
			if (StringUtils.hasText(controlFile.getGploadOutputTable())) {
				table = controlFile.getGploadOutputTable();
			}
			if (controlFile.getGploadOutputMatchColumns() != null) {
				matchColumns = controlFile.getGploadOutputMatchColumns();
			}
			if (controlFile.getGploadOutputUpdateColumns() != null) {
				updateColumns = controlFile.getGploadOutputUpdateColumns();
			}
			if (StringUtils.hasText(controlFile.getGploadOutputUpdateCondition())) {
				updateCondition = controlFile.getGploadOutputUpdateCondition();
			}
			if (!controlFile.getGploadSqlBefore().isEmpty()) {
				sqlBefore = controlFile.getGploadSqlBefore();
			}
			if (!controlFile.getGploadSqlAfter().isEmpty()) {
				sqlAfter = controlFile.getGploadSqlAfter();
			}
		}
	}

	@Override
	public LoadConfiguration getObject() throws Exception {
		LoadConfiguration loadConfiguration = new LoadConfiguration(table, columns, externalTable, mode, matchColumns,
				updateColumns, updateCondition);
		loadConfiguration.setSqlBefore(sqlBefore);
		loadConfiguration.setSqlAfter(sqlAfter);
		return loadConfiguration;
	}

	@Override
	public Class<LoadConfiguration> getObjectType() {
		return LoadConfiguration.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

	public void setControlFile(ControlFile controlFile) {
		this.controlFile = controlFile;
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

	public void setMatchColumns(String[] matchColumns) {
		this.matchColumns = Arrays.asList(matchColumns);
	}

	public List<String> getUpdateColumns() {
		return updateColumns;
	}

	public void setUpdateColumns(String[] updateColumns) {
		this.updateColumns = Arrays.asList(updateColumns);
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
