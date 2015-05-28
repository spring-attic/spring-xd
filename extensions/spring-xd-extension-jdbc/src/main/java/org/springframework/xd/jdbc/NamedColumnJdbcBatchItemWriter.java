/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.xd.jdbc;

import java.util.Arrays;

import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.util.Assert;

/**
 * JdbcBatchItemWriter which is customized for use with the XD
 * JDBC import job.
 *
 * Sets the SQL insert property based on the column names and table name
 * supplied in the module configuration.
 *
 * @author Luke Taylor
 * @author Gary Russell
 */
public class NamedColumnJdbcBatchItemWriter<T> extends JdbcBatchItemWriter<T> {

	private String[] names;

	private String tableName;

	public void setColumnNames(String[] names) {
		this.names = Arrays.copyOf(names, names.length);
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notEmpty(names, "columnNames must be set");
		StringBuilder columns = new StringBuilder();
		StringBuilder namedParams = new StringBuilder();
		for (String column : names) {
			if (columns.length() > 0) {
				columns.append(", ");
				namedParams.append(", ");
			}
			columns.append(column);
			namedParams.append(":").append(column.trim());
		}
		setSql("insert into " + tableName + "(" + columns.toString() + ") values (" + namedParams.toString() + ")");
		super.afterPropertiesSet();
	}
}
