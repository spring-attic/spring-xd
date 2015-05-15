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

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

public class LoadConfigurationFactoryBean implements FactoryBean<LoadConfiguration>, InitializingBean {

	private ControlFile controlFile;

	private String table;

	private String columns;

	private ReadableTable externalTable;

	private Mode mode = Mode.INSERT;

	private List<String> matchColumns;

	private List<String> updateColumns;

	@Override
	public void afterPropertiesSet() throws Exception {
		if (controlFile != null) {
			if (StringUtils.hasText(controlFile.getGploadOutputTable())) {
				table = controlFile.getGploadOutputTable();
			}
		}
	}

	@Override
	public LoadConfiguration getObject() throws Exception {
		return new LoadConfiguration(table, columns, externalTable, mode, matchColumns, updateColumns);
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

	public void setMatchColumns(List<String> matchColumns) {
		this.matchColumns = matchColumns;
	}

	public List<String> getUpdateColumns() {
		return updateColumns;
	}

	public void setUpdateColumns(List<String> updateColumns) {
		this.updateColumns = updateColumns;
	}

}
