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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.hadoop.store.naming.ChainedFileNamingStrategy;
import org.springframework.data.hadoop.store.naming.FileNamingStrategy;
import org.springframework.data.hadoop.store.naming.StaticFileNamingStrategy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.integration.hadoop.IntegrationHadoopSystemConstants;

/**
 * Tests for the 'naming-policy' element.
 * 
 * @author Janne Valkealahti
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class NamingPolicyNamespaceTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testDefault() {
		FileNamingStrategy policy = context.getBean(IntegrationHadoopSystemConstants.DEFAULT_ID_FILE_NAMING_POLICY,
				FileNamingStrategy.class);
		assertNotNull(policy);
		assertThat(policy, instanceOf(StaticFileNamingStrategy.class));
	}

	@Test
	public void testNestedStrategies() {
		FileNamingStrategy policy = context.getBean("nested", FileNamingStrategy.class);
		assertNotNull(policy);
		assertThat(policy, instanceOf(ChainedFileNamingStrategy.class));

		List<? extends FileNamingStrategy> strategies = ((ChainedFileNamingStrategy) policy).getStrategies();
		assertNotNull(strategies);
		assertThat(strategies.size(), is(4));
	}

}
