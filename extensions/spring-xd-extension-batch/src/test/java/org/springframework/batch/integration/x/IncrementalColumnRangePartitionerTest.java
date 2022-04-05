/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.batch.integration.x;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.MapJobExplorerFactoryBean;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IncrementalColumnRangePartitionerConfiguration.class)
public class IncrementalColumnRangePartitionerTest {

	private IncrementalColumnRangePartitioner partitioner;

	private DataSource dataSource;

	private JdbcTemplate jdbc;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		this.jdbc = new JdbcTemplate(dataSource);
	}

	@Before
	public void setUp() {
		partitioner = new IncrementalColumnRangePartitioner();
		partitioner.setDataSource(dataSource);
		jdbc.execute("create table bar (foo int)");
	}

	@After
	public void tearDown() {
		jdbc.execute("drop table bar");
	}

	@Test
	public void testNoPartitions() {
		partitioner.setPartitions(1);
		partitioner.beforeStep(new StepExecution("step1", new JobExecution(5l)));
		Map<String, ExecutionContext> partitions = partitioner.partition(1);
		assertEquals(1, partitions.size());
		assertTrue(partitions.containsKey("partition0"));
		assertEquals("", partitions.get("partition0").get("partClause"));
		assertEquals("", partitions.get("partition0").get("partSuffix"));
	}

	@Test
	public void testTwoPartitions() {
		jdbc.execute("insert into bar (foo) values (1), (2), (3), (4)");
		partitioner.setColumn("foo");
		partitioner.setTable("bar");
		partitioner.setPartitions(2);
		partitioner.beforeStep(new StepExecution("step1", new JobExecution(5l)));
		Map<String, ExecutionContext> partitions = partitioner.partition(1);
		assertEquals(2, partitions.size());
		assertTrue(partitions.containsKey("partition0"));
		assertEquals("WHERE (foo BETWEEN 1 AND 2)", partitions.get("partition0").get("partClause"));
		assertEquals("-p0", partitions.get("partition0").get("partSuffix"));
		assertTrue(partitions.containsKey("partition1"));
		assertEquals("WHERE (foo BETWEEN 3 AND 4)", partitions.get("partition1").get("partClause"));
		assertEquals("-p1", partitions.get("partition1").get("partSuffix"));
	}

	@Test
	public void testFivePartitions() {
		jdbc.execute("insert into bar (foo) values (1), (2), (3), (4), (5)");
		partitioner.setColumn("foo");
		partitioner.setTable("bar");
		partitioner.setPartitions(5);
		partitioner.beforeStep(new StepExecution("step1", new JobExecution(5l)));
		Map<String, ExecutionContext> partitions = partitioner.partition(1);
		assertEquals(5, partitions.size());
		assertTrue(partitions.containsKey("partition4"));
		assertEquals("WHERE (foo BETWEEN 5 AND 5)", partitions.get("partition4").get("partClause"));
		assertEquals("-p4", partitions.get("partition4").get("partSuffix"));
	}


	@Test
	public void testTwoPartitionsForSql() throws Throwable {
		jdbc.execute("insert into bar (foo) values (1), (2), (3), (4)");
		partitioner.setCheckColumn("foo");
		partitioner.setColumn("foo");
		partitioner.setSql("select * from bar");
		partitioner.setPartitions(2);
		SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
		MapJobRepositoryFactoryBean repositoryFactory = new MapJobRepositoryFactoryBean();
		repositoryFactory.afterPropertiesSet();
		JobRepository jobRepository = repositoryFactory.getObject();
		jobLauncher.setJobRepository(jobRepository);
		jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
		jobLauncher.afterPropertiesSet();
		JobExplorer jobExplorer = new MapJobExplorerFactoryBean(repositoryFactory).getObject();
		partitioner.setJobExplorer(jobExplorer);
		JobExecution jobExec = new JobExecution(5l);
		JobInstance jobInstance = new JobInstance(1l, "testIncrementalJDBCSqlJob");
		jobExec.setJobInstance(jobInstance);
		StepExecution stepExec = new StepExecution("step1", jobExec);
		List<StepExecution> stepExecutions = new ArrayList<StepExecution>();
		stepExecutions.add(stepExec);
		jobExec.addStepExecutions(stepExecutions);

		partitioner.beforeStep(new StepExecution("step1", jobExec));
		Map<String, ExecutionContext> partitions = partitioner.partition(1);
		assertEquals(2, partitions.size());
		assertTrue(partitions.containsKey("partition0"));
		String part1Expected = "WHERE (foo BETWEEN 1 AND 2) AND foo > " + Long.MIN_VALUE;
		String part1Actual = (String) partitions.get("partition0").get("partClause");
		assertEquals(part1Expected, part1Actual);
		assertEquals("-p0", partitions.get("partition0").get("partSuffix"));
		assertTrue(partitions.containsKey("partition1"));
		String part2Expected = "WHERE (foo BETWEEN 3 AND 4) AND foo > " + Long.MIN_VALUE;
		String part2Actual = (String) partitions.get("partition1").get("partClause");
		assertEquals(part2Expected, part2Actual);
		assertEquals("-p1", partitions.get("partition1").get("partSuffix"));
	}


	@Test
	public void testIncrementalSqlNextIterationValue() throws Throwable {
		jdbc.execute("insert into bar (foo) values (1), (2), (3), (4)");
		partitioner.setCheckColumn("foo");
		partitioner.setColumn("foo");
		partitioner.setSql("select * from bar");
		partitioner.setPartitions(2);
		JobExplorer jobExplorer = mock(JobExplorer.class);
		partitioner.setJobExplorer(jobExplorer);
		JobExecution jobExec = new JobExecution(1l);
		JobInstance jobInstance1 = new JobInstance(1l, "testIncrementalJDBCSqlJob");
		jobExec.setJobInstance(jobInstance1);
		StepExecution stepExecution = new StepExecution("step1", jobExec);

		when(jobExplorer.getJobInstances("testIncrementalJDBCSqlJob", 1, 1)).thenReturn(new ArrayList<JobInstance>());
		partitioner.beforeStep(stepExecution);
		//first time the value is long minimum as there is no previous instance
		Map<String, ExecutionContext> partitions = partitioner.partition(1);
		String queryPartClause = (String) partitions.get("partition0").get("partClause");
		assertTrue(queryPartClause.endsWith(Long.MIN_VALUE + ""));
		//mark end of job and adjust the max
		jobExec.setEndTime(new Date(System.currentTimeMillis()));
		jobExec.getExecutionContext().put("batch.incremental.maxId", 4l);

		jdbc.execute("insert into bar (foo) values (5), (6), (7), (8)");

		List<JobInstance> jobInstances = new ArrayList<JobInstance>();
		jobInstances.add(jobInstance1);
		JobInstance jobInstance2 = new JobInstance(2l, "testIncrementalJDBCSqlJob");
		jobExec.setJobInstance(jobInstance2);
		jobInstances.add(jobInstance2);
		when(jobExplorer.getJobInstances("testIncrementalJDBCSqlJob", 1, 1)).thenReturn(jobInstances);
		List<JobExecution> executions = new ArrayList<JobExecution>();
		executions.add(jobExec);
		when(jobExplorer.getJobExecutions(jobInstance2)).thenReturn(executions);
		partitioner.beforeStep(new StepExecution("step1", jobExec));
		//this time the value should be 4
		partitions = partitioner.partition(1);
		queryPartClause = (String) partitions.get("partition0").get("partClause");
		assertTrue(queryPartClause.endsWith(4l + ""));
		//mark end of job and adjust the max
		jobExec.setEndTime(new Date(System.currentTimeMillis()));
		jobExec.getExecutionContext().put("batch.incremental.maxId", 8l);

		jdbc.execute("insert into bar (foo) values (9), (10), (7), (8)");

		JobInstance jobInstance3 = new JobInstance(3l, "testIncrementalJDBCSqlJob");
		jobExec.setJobInstance(jobInstance3);
		jobInstances.add(jobInstance3);
		when(jobExplorer.getJobInstances("testIncrementalJDBCSqlJob", 1, 1)).thenReturn(jobInstances);
		executions.add(jobExec);
		when(jobExplorer.getJobExecutions(jobInstance3)).thenReturn(executions);
		partitioner.beforeStep(new StepExecution("step1", jobExec));
		//this time the value should be 4
		partitions = partitioner.partition(1);
		queryPartClause = (String) partitions.get("partition0").get("partClause");
		assertTrue(queryPartClause.endsWith(8l + ""));
		//mark end of job and adjust the max
		jobExec.setEndTime(new Date(System.currentTimeMillis()));
		jobExec.getExecutionContext().put("batch.incremental.maxId", 8l);
	}
}
