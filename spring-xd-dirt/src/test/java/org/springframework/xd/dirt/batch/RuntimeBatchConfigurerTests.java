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

package org.springframework.xd.dirt.batch;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;


/**
 * @author sshcherbakov
 */
public class RuntimeBatchConfigurerTests {

	/**
	 * Test method for {@link org.springframework.xd.dirt.batch.RuntimeBatchConfigurer#createJobRepository()}.
	 * @throws Exception
	 */
	@Test
	public void testCreateJobRepository() throws Exception {

		final String TEST_ISOLATION_LEVEL = "TEST_ISOLATION_LEVEL";
		final int TEST_CLOB_TYPE = 5;
		final String TEST_DB_TYPE = "derby";
		final int TEST_MAX_VARCHAR_LENGTH = 3;
		final String TEST_TABLE_PREFIX = "TEST_TABLE_PREFIX";
		final boolean TEST_VALIDATE_TRANSACTION_STATE = false;

		RuntimeBatchConfigurer batchConfigurer = spy(new RuntimeBatchConfigurer());

		JobRepositoryFactoryBean jobRepositoryFactoryBean = mock(JobRepositoryFactoryBean.class);
		when(batchConfigurer.createJobRepositoryFactoryBean()).thenReturn(jobRepositoryFactoryBean);

		batchConfigurer.setIsolationLevel(TEST_ISOLATION_LEVEL);
		batchConfigurer.setClobType(TEST_CLOB_TYPE);
		batchConfigurer.setDbType(TEST_DB_TYPE);
		batchConfigurer.setMaxVarCharLength(TEST_MAX_VARCHAR_LENGTH);
		batchConfigurer.setTablePrefix(TEST_TABLE_PREFIX);
		batchConfigurer.setValidateTransactionState(TEST_VALIDATE_TRANSACTION_STATE);

		batchConfigurer.createJobRepository();

		verify(jobRepositoryFactoryBean).getObject();
		verify(jobRepositoryFactoryBean).setIsolationLevelForCreate(TEST_ISOLATION_LEVEL);
		verify(jobRepositoryFactoryBean).setClobType(TEST_CLOB_TYPE);
		verify(jobRepositoryFactoryBean).setDatabaseType(TEST_DB_TYPE);
		verify(jobRepositoryFactoryBean).setMaxVarCharLength(TEST_MAX_VARCHAR_LENGTH);
		verify(jobRepositoryFactoryBean).setTablePrefix(TEST_TABLE_PREFIX);
		verify(jobRepositoryFactoryBean).setValidateTransactionState(TEST_VALIDATE_TRANSACTION_STATE);

	}
}
