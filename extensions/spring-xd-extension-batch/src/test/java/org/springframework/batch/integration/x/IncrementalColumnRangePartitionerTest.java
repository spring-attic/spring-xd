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

import java.util.Map;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
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

}
