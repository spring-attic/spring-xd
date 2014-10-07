package org.springframework.xd.batch.jdbc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;

@Configuration
public class ColumnRangePartitionerConfiguration {

	@Bean
	DataSource dataSource() {
		return new SingleConnectionDataSource("jdbc:hsqldb:mem:test", "sa", "", true);
	}

}
