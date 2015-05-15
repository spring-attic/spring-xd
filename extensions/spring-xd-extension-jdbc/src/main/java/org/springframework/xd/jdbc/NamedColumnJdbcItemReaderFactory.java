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

package org.springframework.xd.jdbc;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.item.database.AbstractCursorItemReader;
import org.springframework.batch.support.DatabaseType;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory for configuring a {@link org.springframework.xd.jdbc.NamedColumnJdbcItemReader}.  This factory
 * will verify the type of database and configure the appropriate properties so that a streaming cursor is
 * returned.  Specifically, it will configure {@code fetchSize=Integer.MIN_VALUE} for MySql and set
 * {@code verifyCursorPosition=false } for both MySql and SQLite.
 *
 * @author Michael Minella
 * @author Thomas Risberg
 */
public class NamedColumnJdbcItemReaderFactory implements FactoryBean<NamedColumnJdbcItemReader>, InitializingBean {

	private static final Logger log = LoggerFactory.getLogger(NamedColumnJdbcItemReaderFactory.class);

	private DataSource dataSource;

	private String tableName;

	private String columnNames;

	private String partitionClause;

	private String sql;

	private int fetchSize;

	private boolean verifyCursorPosition = true;

	private boolean initialized = false;

	private NamedColumnJdbcItemReader reader;

	private String delimiter;

	@Override
	public NamedColumnJdbcItemReader getObject() throws Exception {

		if (!initialized) {
			throw new IllegalStateException("Properties have not been initalized");
		}
		return reader;
	}

	@Override
	public Class<?> getObjectType() {
		return NamedColumnJdbcItemReader.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (!StringUtils.hasText(sql)) {
			Assert.hasText(tableName, "tableName must be set");
			Assert.hasText(columnNames, "columns must be set");

			String sql;
			if (StringUtils.hasText(partitionClause)) {
				sql = "SELECT " + columnNames + " FROM " + tableName + " " + partitionClause;
			}
			else {
				sql = "SELECT " + columnNames + " FROM " + tableName;
			}
			log.info("Setting SQL to: " + sql);
			setSql(sql);
		}
		else if (StringUtils.hasText(columnNames) || StringUtils.hasText(tableName)) {
			log.warn("You must set either the 'sql' property or 'tableName' and 'columns'.");
		}

		DatabaseType type = DatabaseType.fromMetaData(dataSource);

		switch (type) {
			case MYSQL:
				fetchSize = Integer.MIN_VALUE;
				// MySql doesn't support getRow for a streaming cursor
				verifyCursorPosition = false;
				break;
			case SQLITE:
				fetchSize = AbstractCursorItemReader.VALUE_NOT_SET;
				break;
			default:
				// keep configured fetchSize
		}

		reader = new NamedColumnJdbcItemReader();
		reader.setSql(sql);
		reader.setFetchSize(fetchSize);
		reader.setDataSource(dataSource);
		reader.setVerifyCursorPosition(verifyCursorPosition);
		reader.setDelimiter(delimiter);
		reader.afterPropertiesSet();

		initialized = true;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public void setColumnNames(String columnNames) {
		this.columnNames = columnNames;
	}

	public void setPartitionClause(String partitionClause) {
		this.partitionClause = partitionClause;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	public void setVerifyCursorPosition(boolean verify) {
		this.verifyCursorPosition = verify;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}
}
