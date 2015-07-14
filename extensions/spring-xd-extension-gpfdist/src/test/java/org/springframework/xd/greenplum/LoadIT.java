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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.xd.greenplum.support.GreenplumLoad;
import org.springframework.xd.greenplum.support.LoadConfigurationFactoryBean;
import org.springframework.xd.greenplum.support.Mode;
import org.springframework.xd.greenplum.support.ReadableTable;

public class LoadIT extends AbstractLoadTests {

	@Test
	public void testInsert() {
		context.register(Config1.class, CommonConfig.class);
		context.refresh();
		JdbcTemplate template = context.getBean(JdbcTemplate.class);
		String drop = "DROP TABLE IF EXISTS AbstractLoadTests;";
		String create = "CREATE TABLE AbstractLoadTests (data text);";
		template.execute(drop);
		template.execute(create);

		List<String> data = new ArrayList<String>();
		for (int i = 0; i < 10; i++) {
			data.add("DATA" + i + "\n");
		}

		broadcastData(data);

		GreenplumLoad greenplumLoad = context.getBean(GreenplumLoad.class);
		greenplumLoad.load();

		List<Map<String, Object>> queryForList = template.queryForList("SELECT * from AbstractLoadTests;");
		assertThat(queryForList, notNullValue());
		assertThat(queryForList.size(), is(10));
		List<String> queryData = new ArrayList<String>();
		for (int i = 0; i < 10; i++) {
			queryData.add((String) queryForList.get(i).get("data"));
		}
		assertThat(
				queryData,
				containsInAnyOrder("DATA0", "DATA1", "DATA2", "DATA3", "DATA4", "DATA5", "DATA6", "DATA7", "DATA8",
						"DATA9"));
	}

	@Test
	public void testUpdate() {
		context.register(Config2.class, CommonConfig.class);
		context.refresh();
		JdbcTemplate template = context.getBean(JdbcTemplate.class);
		String drop = "DROP TABLE IF EXISTS AbstractLoadTests;";
		String create = "CREATE TABLE AbstractLoadTests (col1 text, col2 text);";
		template.execute(drop);
		template.execute(create);

		List<String> data = new ArrayList<String>();
		for (int i = 0; i < 10; i++) {
			template.execute("insert into AbstractLoadTests values('DATA" + i + "', 'DATA');");
			data.add("DATA" + i + "\tDATA" + i + "\n");
		}

		broadcastData(data);

		GreenplumLoad greenplumLoad = context.getBean(GreenplumLoad.class);
		greenplumLoad.load();

		List<Map<String, Object>> queryForList = template.queryForList("SELECT * from AbstractLoadTests;");
		assertThat(queryForList, notNullValue());
		assertThat(queryForList.size(), is(10));
		for (int i = 0; i < 10; i++) {
			assertThat(queryForList.get(i).get("col2"), is(queryForList.get(i).get("col1")));
		}
	}

	@Test
	public void testUpdateMultiColumns() {
		context.register(Config3.class, CommonConfig.class);
		context.refresh();
		JdbcTemplate template = context.getBean(JdbcTemplate.class);
		String drop = "DROP TABLE IF EXISTS AbstractLoadTests;";
		String create = "CREATE TABLE AbstractLoadTests (col1 text, col2 text, col3 text);";
		template.execute(drop);
		template.execute(create);

		List<String> data = new ArrayList<String>();
		for (int i = 0; i < 10; i++) {
			template.execute("insert into AbstractLoadTests values('DATA" + i + "', 'DATA', 'DATA');");
			data.add("DATA" + i + "\tDATA" + i + "\tDATA" + i + "\n");
		}

		broadcastData(data);

		GreenplumLoad greenplumLoad = context.getBean(GreenplumLoad.class);
		greenplumLoad.load();

		List<Map<String, Object>> queryForList = template.queryForList("SELECT * from AbstractLoadTests;");
		assertThat(queryForList, notNullValue());
		assertThat(queryForList.size(), is(10));
		for (int i = 0; i < 10; i++) {
			assertThat(queryForList.get(i).get("col2"), is(queryForList.get(i).get("col1")));
			assertThat(queryForList.get(i).get("col3"), is(queryForList.get(i).get("col1")));
		}
	}

	static class Config1 {

		@Bean
		public LoadConfigurationFactoryBean greenplumLoadConfiguration(ReadableTable externalTable) {
			LoadConfigurationFactoryBean factory = new LoadConfigurationFactoryBean();
			factory.setTable("AbstractLoadTests");
			factory.setExternalTable(externalTable);
			factory.setMode(Mode.INSERT);
			return factory;
		}

	}

	static class Config2 {

		@Bean
		public LoadConfigurationFactoryBean greenplumLoadConfiguration(ReadableTable externalTable) {
			LoadConfigurationFactoryBean factory = new LoadConfigurationFactoryBean();
			factory.setTable("AbstractLoadTests");
			factory.setExternalTable(externalTable);
			factory.setMode(Mode.UPDATE);
			factory.setUpdateColumns(new String[] { "col2" });
			factory.setMatchColumns(new String[] { "col1" });
			return factory;
		}

	}

	static class Config3 {

		@Bean
		public LoadConfigurationFactoryBean greenplumLoadConfiguration(ReadableTable externalTable) {
			LoadConfigurationFactoryBean factory = new LoadConfigurationFactoryBean();
			factory.setTable("AbstractLoadTests");
			factory.setExternalTable(externalTable);
			factory.setMode(Mode.UPDATE);
			factory.setUpdateColumns(new String[] { "col2", "col3" });
			factory.setMatchColumns(new String[] { "col1" });
			return factory;
		}

	}

}
