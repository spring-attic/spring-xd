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

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.test.RandomConfigurationSupport;

/**
 * @author sshcherbakov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
@ActiveProfiles(profiles = { "admin", "hsqldbServer" })
public class RuntimeBatchConfigurerIntegrationTests extends RandomConfigurationSupport {

	@Configuration
	@ImportResource("classpath:/META-INF/spring-xd/batch/batch.xml")
	@EnableBatchProcessing
	public static class TestConfig implements BeanPostProcessor {

		private JobRepository testJobRepo = mock(JobRepository.class);

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof BatchConfigurer) {
				try {

					RuntimeBatchConfigurer batchConfigurer = spy((RuntimeBatchConfigurer) bean);
					doReturn(testJobRepo).when(batchConfigurer).getJobRepository();

					return batchConfigurer;
				}
				catch (Exception ex) {
					throw new BeanInitializationException("Cannot initialize proxy", ex);
				}
			}
			return bean;
		}

		@Bean
		public static PropertySourcesPlaceholderConfigurer placeHolderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

		@Bean
		public JdbcTemplate jdbcTemplate(DataSource dataSource) {
			return new JdbcTemplate(dataSource);
		}

		public JobRepository getCustomJobRepo() {
			return testJobRepo;
		}

	}


	@Autowired
	BatchConfigurer batchConfigurer;

	@Autowired
	JobRepository jobRepository;

	@Autowired
	TestConfig testConfig;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	public void testRuntimeBatchConfigurer() throws Exception {

		jobRepository.toString(); // needed to trigger lazy bean initialization

		assertSame(
				getTargetObject(jobRepository, JobRepository.class),
				testConfig.getCustomJobRepo());

	}

	@After
	public void cleanUp() {
		// Need to shut down the HSQL database server started by batch.xml
		jdbcTemplate.execute("SHUTDOWN");
	}

	@SuppressWarnings({ "unchecked" })
	private <T> T getTargetObject(Object proxy, Class<T> targetClass) throws Exception {
		if (AopUtils.isJdkDynamicProxy(proxy)) {
			return (T) ((Advised) proxy).getTargetSource().getTarget();
		}
		else {
			return (T) proxy;
		}
	}

}
