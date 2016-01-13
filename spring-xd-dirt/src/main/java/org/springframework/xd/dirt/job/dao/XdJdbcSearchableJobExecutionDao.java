/*
 * Copyright 2006-2016 the original author or authors.
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

package org.springframework.xd.dirt.job.dao;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.admin.service.JdbcSearchableJobExecutionDao;
import org.springframework.batch.admin.service.SearchableJobExecutionDao;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.repository.dao.JdbcJobExecutionDao;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.incrementer.AbstractDataFieldMaxValueIncrementer;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.batch.tasklet.JobLaunchingTasklet;

/**
 * @author Dave Syer
 * @author Michael Minella
 * @author Gunnar Hillert
 *
 */
public class XdJdbcSearchableJobExecutionDao extends JdbcSearchableJobExecutionDao {

	private static final String FIELDS = "E.JOB_EXECUTION_ID, E.START_TIME, E.END_TIME, E.STATUS, E.EXIT_CODE, E.EXIT_MESSAGE, "
			+ "E.CREATE_TIME, E.LAST_UPDATED, E.VERSION, I.JOB_INSTANCE_ID, I.JOB_NAME";

	private PagingQueryProvider allExecutionsPagingQueryProvider;

	private PagingQueryProvider childJobExecutionsPagingQueryProvider;

	private DataSource dataSource;

	/**
	 * @param dataSource the dataSource to set
	 */
	@Override
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		super.setDataSource(dataSource);
	}

	/**
	 * @see JdbcJobExecutionDao#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {

		Assert.state(dataSource != null, "DataSource must be provided");

		if (getJdbcTemplate() == null) {
			setJdbcTemplate(new JdbcTemplate(dataSource));
		}
		setJobExecutionIncrementer(new AbstractDataFieldMaxValueIncrementer() {

			@Override
			protected long getNextKey() {
				return 0;
			}
		});

		final String subQuery = getQuery(
				"not exists (select * from %PREFIX%JOB_EXECUTION_PARAMS a where key_name = '"
						+ JobLaunchingTasklet.XD_PARENT_JOB_EXECUTION_ID
						+ "' and E.JOB_EXECUTION_ID=a.JOB_EXECUTION_ID)");
		allExecutionsPagingQueryProvider = getPagingQueryProvider(subQuery);

		final String childJobExecutionsSubQuery = getQuery(
				"exists (select * from %PREFIX%JOB_EXECUTION_PARAMS a where key_name = '"
						+ JobLaunchingTasklet.XD_PARENT_JOB_EXECUTION_ID
						+ "' and E.JOB_EXECUTION_ID=a.JOB_EXECUTION_ID and string_val=?)");
		childJobExecutionsPagingQueryProvider = getPagingQueryProvider(childJobExecutionsSubQuery);

		super.afterPropertiesSet();

	}

	/**
	 * @return a {@link PagingQueryProvider} with a where clause to narrow the
	 * query
	 * @throws Exception
	 */
	private PagingQueryProvider getPagingQueryProvider(String whereClause) throws Exception {
		SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();
		factory.setDataSource(dataSource);
		String fromClause = "%PREFIX%JOB_EXECUTION E, %PREFIX%JOB_INSTANCE I";
		factory.setFromClause(getQuery(fromClause));
		factory.setSelectClause(FIELDS);
		Map<String, Order> sortKeys = new HashMap<String, Order>();
		sortKeys.put("JOB_EXECUTION_ID", Order.DESCENDING);
		factory.setSortKeys(sortKeys);
		whereClause = "E.JOB_INSTANCE_ID=I.JOB_INSTANCE_ID" + (whereClause == null ? "" : " and " + whereClause);
		factory.setWhereClause(whereClause);

		return factory.getObject();
	}

	/**
	 * @see SearchableJobExecutionDao#getJobExecutions(int, int)
	 */
	public List<JobExecution> getTopLevelJobExecutions(int start, int count) {
		if (start <= 0) {
			return getJdbcTemplate().query(allExecutionsPagingQueryProvider.generateFirstPageQuery(count),
					new JobExecutionRowMapper());
		}
		try {
			Long startAfterValue = getJdbcTemplate().queryForObject(
					allExecutionsPagingQueryProvider.generateJumpToItemQuery(start, count), Long.class);
			return getJdbcTemplate().query(allExecutionsPagingQueryProvider.generateRemainingPagesQuery(count),
					new JobExecutionRowMapper(), startAfterValue);
		}
		catch (IncorrectResultSizeDataAccessException e) {
			return Collections.emptyList();
		}
	}

	public int countTopLevelJobExecutions() {
		return getJdbcTemplate().queryForObject(getQuery("SELECT COUNT(1) from %PREFIX%JOB_EXECUTION bje where " +
				"not exists (select 1 from %PREFIX%JOB_EXECUTION_PARAMS bje_params where key_name = '"
				+ JobLaunchingTasklet.XD_PARENT_JOB_EXECUTION_ID
				+ "' and bje.JOB_EXECUTION_ID=bje_params.JOB_EXECUTION_ID)"), Integer.class);
	}

	/**
	 *
	 * @param jobExecutionId the execution id.
	 * @return the list of {@link JobExecution}s.
	 */
	public List<JobExecution> getChildJobExecutions(long jobExecutionId) {
		try {
			return getJdbcTemplate().query(childJobExecutionsPagingQueryProvider.generateFirstPageQuery(100000),
					new JobExecutionRowMapper(), String.valueOf(jobExecutionId));
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 *
	 * @param jobExecutionId the execution id.
	 * @return the list of {@link JobExecution}s.
	 */
	public boolean isComposedJobExecution(long jobExecutionId) {
		String query = getQuery("select count(*) from %PREFIX%JOB_EXECUTION_PARAMS a where key_name = '"
				+ JobLaunchingTasklet.XD_PARENT_JOB_EXECUTION_ID + "' and string_val = ?");
		int count = getJdbcTemplate().queryForObject(query, Integer.class, String.valueOf(jobExecutionId));
		return count > 0 ? true : false;
	}

}
