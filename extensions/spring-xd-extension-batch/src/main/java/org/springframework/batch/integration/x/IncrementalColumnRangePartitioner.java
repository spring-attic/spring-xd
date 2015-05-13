/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.batch.integration.x;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

/**
 * Provides the ability to partition a job and append additional conditions to support
 * incremental imports.  Incremental imports are supported via the checkColumn attribute.
 * The partitionMax value processed in the current run will be set as the minimum value for the
 * next run.
 *
 * @author Michael Minella
 * @since 1.2
 */
public class IncrementalColumnRangePartitioner implements Partitioner, StepExecutionListener, InitializingBean {

	private static final Log log = LogFactory.getLog(IncrementalColumnRangePartitioner.class);

	public static final String BATCH_INCREMENTAL_MAX_ID = "batch.incremental.maxId";

	private JdbcOperations jdbcTemplate;

	private String table;

	private String column;

	private int partitions;

	private long partitionMax = Long.MAX_VALUE;

	private long partitionMin = Long.MIN_VALUE;

	private long incrementalMin = Long.MIN_VALUE;

	private JobExplorer jobExplorer;

	private String checkColumn;

	private Long overrideValue;

	/**
	 * The data source for connecting to the database.
	 *
	 * @param dataSource a {@link DataSource}
	 */
	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * The name of the SQL table the data are in.
	 *
	 * @param table the name of the table
	 */
	public void setTable(String table) {
		this.table = table;
	}

	/**
	 * The name of the column to partition.
	 *
	 * @param column the column name.
	 */
	public void setColumn(String column) {
		this.column = column;
	}

	/**
	 * The number of partitions to create.
	 *
	 * @param partitions the number of partitions.
	 */
	public void setPartitions(int partitions) {
		this.partitions = partitions;
	}

	public void setJobExplorer(JobExplorer jobExplorer) {
		this.jobExplorer = jobExplorer;
	}

	public void setCheckColumn(String checkColumn) {
		this.checkColumn = checkColumn;
	}

	public void setOverrideValue(Long overrideValue) {
		this.overrideValue = overrideValue;
	}

	/**
	 * Partition a database table assuming that the data in the column specified
	 * are uniformly distributed. The execution context values will have keys
	 * <code>minValue</code> and <code>maxValue</code> specifying the range of
	 * values to consider in each partition.
	 *
	 * @see Partitioner#partition(int)
	 */
	@Override
	public Map<String, ExecutionContext> partition(int gridSize) {
		StringBuilder incrementalClause = new StringBuilder();
		Map<String, ExecutionContext> result = new HashMap<>();

		if(!StringUtils.hasText(checkColumn) && !StringUtils.hasText(column)) {
			ExecutionContext value = new ExecutionContext();
			value.put("partClause", "");
			result.put("partition0", value);
			value.put("partSuffix", "");
		}
		else {
			if(StringUtils.hasText(checkColumn)) {
				incrementalClause.append(checkColumn).append(" > ").append(this.incrementalMin);
			}

			long targetSize = (this.partitionMax - this.partitionMin) / partitions + 1;

			int number = 0;
			long start = this.partitionMin;
			long end = start + targetSize - 1;

			while (start >= 0 && start <= this.partitionMax) {
				ExecutionContext value = new ExecutionContext();
				result.put("partition" + number, value);

				if (end >= this.partitionMax) {
					end = this.partitionMax;
				}

				if(StringUtils.hasText(checkColumn)) {
					value.putString("partClause", String.format("WHERE (%s BETWEEN %s AND %s) AND %s", column, start, end, incrementalClause.toString()));
				}
				else {
					value.putString("partClause", String.format("WHERE (%s BETWEEN %s AND %s)", column, start, end));
				}

				value.putString("partSuffix", "-p"+number);
				start += targetSize;
				end += targetSize;
				number++;

				log.debug("Current ExecutionContext = " + value);
			}
		}

		return result;
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		if(StringUtils.hasText(checkColumn)) {

			if(overrideValue != null && overrideValue >= 0) {
				this.incrementalMin = overrideValue;
			}
			else {
				String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();

				// Get the last jobInstance...not the current one
				List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, 1, 1);

				if(jobInstances.size() > 0) {
					JobInstance lastInstance = jobInstances.get(jobInstances.size() - 1);

					List<JobExecution> executions = jobExplorer.getJobExecutions(lastInstance);

					JobExecution lastExecution = executions.get(0);

					for (JobExecution execution : executions) {
						if(lastExecution.getEndTime().getTime() < execution.getEndTime().getTime()) {
							lastExecution = execution;
						}
					}

					if(lastExecution.getExecutionContext().containsKey(BATCH_INCREMENTAL_MAX_ID)) {
						this.incrementalMin = lastExecution.getExecutionContext().getLong(BATCH_INCREMENTAL_MAX_ID);
					}
					else {
						this.incrementalMin = Long.MIN_VALUE;
					}
				}
				else {
					this.incrementalMin = Long.MIN_VALUE;
				}
			}

			long newMin = jdbcTemplate.queryForObject(String.format("select max(%s) from %s", checkColumn, table), Integer.class);

			stepExecution.getExecutionContext().put(BATCH_INCREMENTAL_MAX_ID, newMin);
		}

		if(StringUtils.hasText(column) && StringUtils.hasText(table)) {
			if(StringUtils.hasText(checkColumn)) {
				Long minResult = jdbcTemplate.queryForObject("SELECT MIN(" + column + ") from " + table + " where " + checkColumn + " > " + this.incrementalMin, Long.class);
				Long maxResult = jdbcTemplate.queryForObject("SELECT MAX(" + column + ") from " + table + " where " + checkColumn + " > " + this.incrementalMin, Long.class);
				this.partitionMin = minResult != null ? minResult : Long.MIN_VALUE;
				this.partitionMax = maxResult != null ? maxResult : Long.MAX_VALUE;
			}
			else {
				Long minResult = jdbcTemplate.queryForObject("SELECT MIN(" + column + ") from " + table, Long.class);
				Long maxResult = jdbcTemplate.queryForObject("SELECT MAX(" + column + ") from " + table, Long.class);
				this.partitionMin = minResult != null ? minResult : Long.MIN_VALUE;
				this.partitionMax = maxResult != null ? maxResult : Long.MAX_VALUE;
			}
		}
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		return stepExecution.getExitStatus();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if(!StringUtils.hasText(this.column)) {
			this.column = this.checkColumn;
		}
	}
}
