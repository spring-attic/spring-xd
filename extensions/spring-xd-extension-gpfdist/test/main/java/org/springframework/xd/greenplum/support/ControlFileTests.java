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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

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
