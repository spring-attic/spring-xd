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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.xd.greenplum.support.ControlFile.OutputMode;

public class ControlFileTests {

	private AnnotationConfigApplicationContext context;

	@Test
	public void testLoadFromFactory() {
		context = new AnnotationConfigApplicationContext();
		context.register(Config1.class);
		context.refresh();

		ControlFile cf = context.getBean(ControlFile.class);
		assertThat(cf.getGploadOutputTable(), is("test"));
		assertThat(cf.getGploadInputDelimiter(), is(','));
		assertThat(cf.getDatabase(), is("gpadmin"));
		assertThat(cf.getUser(), is("gpadmin"));
		assertThat(cf.getHost(), is("mdw.example.org"));
		assertThat(cf.getPort(), is(5432));
		assertThat(cf.getPassword(), nullValue());

		assertThat(cf.getGploadOutputMode(), is(OutputMode.UPDATE));

		assertThat(cf.getGploadOutputMatchColumns(), notNullValue());
		assertThat(cf.getGploadOutputMatchColumns().size(), is(2));
		assertThat(cf.getGploadOutputMatchColumns().get(0), is("col11"));
		assertThat(cf.getGploadOutputMatchColumns().get(1), is("col12"));

		assertThat(cf.getGploadOutputUpdateColumns(), notNullValue());
		assertThat(cf.getGploadOutputUpdateColumns().size(), is(2));
		assertThat(cf.getGploadOutputUpdateColumns().get(0), is("col21"));
		assertThat(cf.getGploadOutputUpdateColumns().get(1), is("col22"));
		assertThat(cf.getGploadOutputUpdateCondition(), is("condition"));

		assertThat(cf.getGploadSqlBefore().get(0), is("select 1 as before"));
		assertThat(cf.getGploadSqlBefore().get(1), is("select 2 as before"));
		assertThat(cf.getGploadSqlAfter().get(0), is("select 1 as after"));
		assertThat(cf.getGploadSqlAfter().get(1), is("select 2 as after"));
	}

	static class Config1 {

		@Bean
		public ControlFileFactoryBean controlFile() {
			ControlFileFactoryBean f = new ControlFileFactoryBean();
			f.setControlFileResource(new ClassPathResource("test.yml"));
			return f;
		}

	}

	@After
	public void clean() {
		context.close();
		context = null;
	}

}
