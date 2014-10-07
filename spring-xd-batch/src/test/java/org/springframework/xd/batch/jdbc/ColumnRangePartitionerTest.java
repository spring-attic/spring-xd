package org.springframework.xd.batch.jdbc;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ColumnRangePartitionerConfiguration.class)
public class ColumnRangePartitionerTest {

	private ColumnRangePartitioner partitioner;

	private DataSource dataSource;

	private JdbcTemplate jdbc;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		this.jdbc = new JdbcTemplate(dataSource);
	}

	@Before
	public void setUp() {
		partitioner = new ColumnRangePartitioner();
		partitioner.setDataSource(dataSource);
	}

	@Test
	public void testNoPartitions() {
		partitioner.setPartitions(1);
		Map<String, ExecutionContext> partitions = partitioner.partition(1);
		assertEquals(1, partitions.size());
		assertTrue(partitions.containsKey("partition0"));
		assertEquals("", partitions.get("partition0").get("partClause"));
		assertEquals("", partitions.get("partition0").get("partSuffix"));
	}

	@Test
	public void testTwoPartitions() {
		jdbc.execute("create table bar (foo int)");
		jdbc.execute("insert into bar (foo) values (1), (2), (3), (4)");
		partitioner.setColumn("foo");
		partitioner.setTable("bar");
		partitioner.setPartitions(2);
		Map<String, ExecutionContext> partitions = partitioner.partition(1);
		assertEquals(2, partitions.size());
		assertTrue(partitions.containsKey("partition0"));
		assertEquals("WHERE foo BETWEEN 1 AND 2", partitions.get("partition0").get("partClause"));
		assertEquals("-p0", partitions.get("partition0").get("partSuffix"));
		assertTrue(partitions.containsKey("partition1"));
		assertEquals("WHERE foo BETWEEN 3 AND 4", partitions.get("partition1").get("partClause"));
		assertEquals("-p1", partitions.get("partition1").get("partSuffix"));
		jdbc.execute("drop table bar");
	}

	@Test
	public void testFivePartitions() {
		jdbc.execute("create table bar (foo int)");
		jdbc.execute("insert into bar (foo) values (1), (2), (3), (4), (5)");
		partitioner.setColumn("foo");
		partitioner.setTable("bar");
		partitioner.setPartitions(5);
		Map<String, ExecutionContext> partitions = partitioner.partition(1);
		assertEquals(5, partitions.size());
		assertTrue(partitions.containsKey("partition4"));
		assertEquals("WHERE foo BETWEEN 5 AND 5", partitions.get("partition4").get("partClause"));
		assertEquals("-p4", partitions.get("partition4").get("partSuffix"));
		jdbc.execute("drop table bar");
	}

}
