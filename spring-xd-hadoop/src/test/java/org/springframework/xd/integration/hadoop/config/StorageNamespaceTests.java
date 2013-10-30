/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.integration.hadoop.config;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.hadoop.store.text.DelimitedTextStorage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.integration.hadoop.IntegrationHadoopSystemConstants;

/**
 * Tests for the 'storage' element.
 * 
 * @author Janne Valkealahti
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class StorageNamespaceTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testDefaults() {
		DelimitedTextStorage storage = context.getBean(IntegrationHadoopSystemConstants.DEFAULT_ID_STORAGE,
				DelimitedTextStorage.class);
		assertThat(storage, notNullValue());
	}

	@Test
	public void testCodecs() {
		DelimitedTextStorage storage1 = context.getBean("bzip2Codec",
				DelimitedTextStorage.class);
		assertThat(storage1, notNullValue());
		assertThat(storage1.getCodec(), notNullValue());

		DelimitedTextStorage storage2 = context.getBean("gzipCodec", DelimitedTextStorage.class);
		assertThat(storage2, notNullValue());
		assertThat(storage2.getCodec(), notNullValue());

		DelimitedTextStorage storage3 = context.getBean("wrongCodec", DelimitedTextStorage.class);
		assertThat(storage3, notNullValue());
		assertThat(storage3.getCodec(), nullValue());

		DelimitedTextStorage storage4 = context.getBean("emptyCodec", DelimitedTextStorage.class);
		assertThat(storage4, notNullValue());
		assertThat(storage4.getCodec(), nullValue());

	}

}
