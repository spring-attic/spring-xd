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
import java.util.ListIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class CleanableJdbcOperations {

	private static final Log log = LogFactory.getLog(CleanableJdbcOperations.class);

	private JdbcTemplate jdbcTemplate;
	private List<Operation> operations;
	private String runSql;
	private DataAccessException createException;
	private DataAccessException cleanException;

	public CleanableJdbcOperations(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.operations = new ArrayList<CleanableJdbcOperations.Operation>();
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void add(String createSql, String cleanSql) {
		operations.add(new Operation(createSql, cleanSql));
	}

	public void setRunSql(String sql) {
		this.runSql = sql;
	}

	public String getRunSql() {
		return runSql;
	}

	private class Operation {
		String createSql;
		String cleanSql;

		public Operation(String createSql, String cleanSql) {
			super();
			this.createSql = createSql;
			this.cleanSql = cleanSql;
		}
	}

	public void prepare() {
		for (Operation operation : operations) {
			try {
				log.info(operation.createSql);
				jdbcTemplate.execute(operation.createSql);
			} catch (DataAccessException e) {
				createException = e;
				return;
			}
		}
	}

	public void clean() {
		ListIterator<Operation> listIterator = operations.listIterator(operations.size());
		while (listIterator.hasPrevious()) {
			Operation operation = listIterator.previous();
			try {
				log.info(operation.cleanSql);
				jdbcTemplate.execute(operation.cleanSql);
			} catch (DataAccessException e) {
				cleanException = e;
			}
		}
	}

	public DataAccessException getCreateException() {
		return createException;
	}

	public DataAccessException getCleanException() {
		return cleanException;
	}

}
