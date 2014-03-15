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

package org.springframework.xd.shell.command;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.xd.shell.command.fixtures.HttpSource;
import org.springframework.xd.test.fixtures.JdbcSink;


/**
 * Tests for the jdbc related modules.
 * 
 * @author Eric Bottard
 * @author Florent Biville
 */
public class JdbcModulesTests extends AbstractStreamIntegrationTest {

	@Test
	public void testJdbcSinkWith1InsertionAndDefaultConfiguration() throws Exception {
		JdbcSink jdbcSink = newJdbcSink().start();

		HttpSource httpSource = newHttpSource();


		String streamName = generateStreamName().replaceAll("-", "_");
		stream().create(streamName, "%s | %s", httpSource, jdbcSink);
		httpSource.ensureReady().postData("Hi there!");

		String query = String.format("SELECT payload FROM %s", streamName);
		assertEquals(
				"Hi there!",
				jdbcSink.getJdbcTemplate().queryForObject(query, String.class));
	}

	@Test
	public void testJdbcSinkWith2InsertionsAndDefaultConfiguration() throws Exception {
		JdbcSink jdbcSink = newJdbcSink().start();

		HttpSource httpSource = newHttpSource();


		String streamName = generateStreamName().replaceAll("-", "_");
		stream().create(streamName, "%s | %s", httpSource, jdbcSink);
		httpSource.ensureReady().postData("Hi there!");
		httpSource.postData("How are you?");

		String query = String.format("SELECT payload FROM %s", streamName);
		List<String> result = jdbcSink.getJdbcTemplate().queryForList(query, String.class);
		assertThat(result, contains("Hi there!", "How are you?"));
	}

	@Test
	public void testJdbcSinkWithCustomTableName() throws Exception {
		String tableName = "foobar";
		JdbcSink jdbcSink = newJdbcSink().tableName(tableName).start();


		HttpSource httpSource = newHttpSource();

		stream().create(generateStreamName(), "%s | %s", httpSource, jdbcSink);
		httpSource.ensureReady().postData("Hi there!");

		String query = String.format("SELECT payload FROM %s", tableName);
		assertEquals(
				"Hi there!",
				jdbcSink.getJdbcTemplate().queryForObject(query, String.class));
	}

	@Test
	public void testJdbcSinkWithCustomColumnNames() throws Exception {
		JdbcSink jdbcSink = newJdbcSink().columns("foo,bar").start();

		HttpSource httpSource = newHttpSource();


		String streamName = generateStreamName().replaceAll("-", "_");
		stream().create(streamName, "%s | %s", httpSource, jdbcSink);
		String json = "{\"foo\":5, \"bar\": \"hello\"}";
		httpSource.ensureReady().postData(json);

		String query = String.format("SELECT foo, bar FROM %s", streamName);
		Result result = jdbcSink.getJdbcTemplate().queryForObject(query, new BeanPropertyRowMapper<>(Result.class));
		assertEquals("hello", result.getBar());
		assertEquals(5, result.getFoo());
	}

	@Test
	public void testJdbcSinkWithCustomColumnNamesWithUnderscores() throws Exception {
		JdbcSink jdbcSink = newJdbcSink().columns("foo_bar,bar_foo,bar_baz").start();

		HttpSource httpSource = newHttpSource();

		String streamName = generateStreamName().replaceAll("-", "_");
		stream().create(streamName, "%s | %s", httpSource, jdbcSink);
		String json = "{\"fooBar\":5, \"barFoo\": \"hello\", \"bar_baz\": \"good_bye\"}";
		httpSource.ensureReady().postData(json);

		String query = String.format("SELECT foo_bar, bar_foo, bar_baz FROM %s", streamName);
		Map<String, Object> result = jdbcSink.getJdbcTemplate().queryForMap(query);
		assertEquals("hello", result.get("bar_foo"));
		assertEquals("5", result.get("foo_bar"));
		assertEquals("good_bye", result.get("bar_baz"));
	}

	public static class Result {

		private String bar;

		private int foo;


		public String getBar() {
			return bar;
		}


		public int getFoo() {
			return foo;
		}


		public void setBar(String bar) {
			this.bar = bar;
		}


		public void setFoo(int foo) {
			this.foo = foo;
		}


	}

}
