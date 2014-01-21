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

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.Assert;

/**
 * Class that's intended for initializing a single table and the script used can use a placeholder #table for the table
 * name. We'll replace the placeholder with the specified table name when the context is initialized.
 * 
 * @author Thomas Risberg
 * @since 1.0
 */
public class SingleTableDatabaseInitializer extends ResourceDatabasePopulator implements InitializingBean {

	private static final Log logger = LogFactory.getLog(SingleTableDatabaseInitializer.class);

	private List<Resource> scripts = new ArrayList<Resource>();

	private static final String TABLE_PLACEHOLDER = "#table";

	private String tableName = null;

	private static final String COLUMNS_PLACEHOLDER = "#columns";

	private String columns = "payload varchar(2000)";

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	@Override
	public void addScript(Resource script) {
		this.scripts.add(script);
	}

	@Override
	public void setScripts(Resource... scripts) {
		this.scripts = Arrays.asList(scripts);
	}

	/**
	 * Sets the column names which will be used to create a DDL definition
	 * of the columns for the table.
	 */
	public void setColumnNames(String[] names) {
		Assert.notEmpty(names, "columnNames cannot be empty");
		StringBuilder columns = new StringBuilder();
		for (String column : names) {
			if (columns.length() > 0) {
				columns.append(", ");
			}
			columns.append(column).append(" varchar(2000) ");
		}
		this.columns = columns.toString();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		for (Resource script : this.scripts) {
			super.addScript(substitutePlaceholdersForResource(script));
		}
	}

	private Resource substitutePlaceholdersForResource(Resource resource) {

		StringBuilder script = new StringBuilder();

		try {
			EncodedResource er = new EncodedResource(resource);
			LineNumberReader lnr = new LineNumberReader(er.getReader());
			String line = lnr.readLine();
			while (line != null) {
				if (tableName != null && line.contains(TABLE_PLACEHOLDER)) {
					logger.debug("Substituting '" + TABLE_PLACEHOLDER + "' with '" + tableName + "' in '" + line + "'");
					line = line.replace(TABLE_PLACEHOLDER, tableName);
				}
				if (line.contains(COLUMNS_PLACEHOLDER)) {
					logger.debug("Substituting '" + COLUMNS_PLACEHOLDER + "' with '" + columns + "' in '" + line + "'");
					line = line.replace(COLUMNS_PLACEHOLDER, columns);
				}
				script.append(line + "\n");
				line = lnr.readLine();
			}
			lnr.close();
			return new ByteArrayResource(script.toString().getBytes());
		}
		catch (IOException e) {
			throw new InvalidDataAccessResourceUsageException("Unable to read script " + resource, e);
		}
	}
}
