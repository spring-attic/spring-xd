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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

/**
 * Utility class helping to execute jdbc operations within a load session.
 * Provides a way to prepare, run and clean a main load command. Additionally
 * it can use a list of before and after commands which are execute before and after
 * of a main command. Clean command is executed last even if some of the other
 * commands fail.
 *
 * @author Janne Valkealahti
 *
 */
public class JdbcCommands {

	private static final Log log = LogFactory.getLog(JdbcCommands.class);

	private JdbcTemplate jdbcTemplate;

	private List<String> beforeSqls;

	private List<String> afterSqls;

	private String prepareSql;

	private String runSql;

	private String cleanSql;

	private DataAccessException lastException;

	public JdbcCommands(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void setPrepareSql(String sql) {
		this.prepareSql = sql;
	}

	public void setRunSql(String sql) {
		this.runSql = sql;
	}

	public void setCleanSql(String sql) {
		this.cleanSql = sql;
	}

	public void setBeforeSqls(List<String> beforeSqls) {
		this.beforeSqls = beforeSqls;
	}

	public void setAfterSqls(List<String> afterSqls) {
		this.afterSqls = afterSqls;
	}

	public boolean execute() {
		boolean succeed = true;

		try {
			succeed = prepare();
			if (succeed) {
				succeed = before();
			}
			if (succeed) {
				succeed = run();
			}
			if (succeed) {
				succeed = after();
			}
		}
		catch (Exception e) {
		}
		finally {
			try {
				clean();
			}
			catch (Exception e2) {
			}
		}
		return succeed;
	}

	public DataAccessException getLastException() {
		return lastException;
	}

	private boolean prepare() {
		try {
			if (log.isDebugEnabled()) {
				log.debug("Executing prepare: " + prepareSql);
			}
			jdbcTemplate.execute(prepareSql);
		}
		catch (DataAccessException e) {
			lastException = e;
			return false;
		}
		return true;
	}

	private boolean run() {
		try {
			if (log.isDebugEnabled()) {
				log.debug("Executing run: " + runSql);
			}
			jdbcTemplate.execute(runSql);
		}
		catch (DataAccessException e) {
			lastException = e;
			return false;
		}
		return true;
	}

	private boolean clean() {
		try {
			if (log.isDebugEnabled()) {
				log.debug("Executing clean: " + cleanSql);
			}
			jdbcTemplate.execute(cleanSql);
		}
		catch (DataAccessException e) {
			lastException = e;
			return false;
		}
		return true;
	}

	private boolean before() {
		if (beforeSqls != null) {
			for (String sql : beforeSqls) {
				if (!StringUtils.hasText(sql)) {
					continue;
				}
				if (log.isDebugEnabled()) {
					log.debug("Executing before: " + sql);
				}
				try {
					jdbcTemplate.execute(sql);
				}
				catch (DataAccessException e) {
					lastException = e;
					return false;
				}
			}
		}
		return true;
	}

	private boolean after() {
		if (afterSqls != null) {
			for (String sql : afterSqls) {
				if (!StringUtils.hasText(sql)) {
					continue;
				}
				if (log.isDebugEnabled()) {
					log.debug("Executing after: " + sql);
				}
				try {
					jdbcTemplate.execute(sql);
				}
				catch (DataAccessException e) {
					lastException = e;
					return false;
				}
			}
		}
		return true;
	}

}
