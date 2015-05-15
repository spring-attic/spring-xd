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
package org.springframework.xd.greenplum;

import java.util.Arrays;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.xd.greenplum.support.Format;
import org.springframework.xd.greenplum.support.GreenplumLoad;
import org.springframework.xd.greenplum.support.LoadConfiguration;
import org.springframework.xd.greenplum.support.LoadConfigurationFactoryBean;
import org.springframework.xd.greenplum.support.LoadFactoryBean;
import org.springframework.xd.greenplum.support.Mode;
import org.springframework.xd.greenplum.support.ReadableTable;
import org.springframework.xd.greenplum.support.ReadableTableFactoryBean;

public class SimpleLoadTests {

	private AnnotationConfigApplicationContext context;

//	@Test
	public void testSimpleLoad() {
		GreenplumLoad load = context.getBean(GreenplumLoad.class);
		load.load();
	}

	static class Config {

		@Bean
		public LoadFactoryBean greenplumLoad(LoadConfiguration loadConfiguration) {
			LoadFactoryBean factory = new LoadFactoryBean();
			factory.setLoadConfiguration(loadConfiguration);
			factory.setDataSource(dataSource());
			return factory;
		}

		@Bean
		public LoadConfigurationFactoryBean greenplumLoadConfiguration(ReadableTable externalTable) {
			LoadConfigurationFactoryBean factory = new LoadConfigurationFactoryBean();
			factory.setTable("test");
			factory.setExternalTable(externalTable);
			factory.setMode(Mode.INSERT);
			return factory;
		}

		@Bean
		public ReadableTableFactoryBean greenplumReadableTable() {
			ReadableTableFactoryBean factory = new ReadableTableFactoryBean();
			factory.setLocations(Arrays.asList("gpfdist://mdw:8082/test1.txt"));
			factory.setFormat(Format.TEXT);
			factory.setDelimiter(',');
			return factory;
		}

		@Bean
		public JdbcTemplate jdbcTemplate() {
			return new JdbcTemplate(dataSource());
		}

		@Bean
		public BasicDataSource dataSource() {
			BasicDataSource dataSource = new BasicDataSource();
			dataSource.setDriverClassName("org.postgresql.Driver");
			dataSource.setUrl("jdbc:postgresql://mdw/gpadmin");
			dataSource.setUsername("gpadmin");
			dataSource.setPassword("gpadmin");
			return dataSource;
		}

	}

//	@Before
	public void setup() {
		context = new AnnotationConfigApplicationContext();
		context.register(Config.class);
		context.refresh();
	}

//	@After
	public void clean() {
		context.close();
		context = null;
	}

}
