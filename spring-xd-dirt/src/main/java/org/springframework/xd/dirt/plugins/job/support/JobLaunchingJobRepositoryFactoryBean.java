/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.xd.dirt.plugins.job.support;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * A {@link org.springframework.beans.factory.FactoryBean} for creating an instance of
 * {@link org.springframework.xd.dirt.plugins.job.support.JobLaunchingJobRepository}
 *
 * @author Michael Minella
 */
public class JobLaunchingJobRepositoryFactoryBean extends JobRepositoryFactoryBean {

	private PlatformTransactionManager transactionManager;
	private static final String DEFAULT_ISOLATION_LEVEL = "ISOLATION_READ_COMMITTED";
	private String isolationLevelForCreate = DEFAULT_ISOLATION_LEVEL;
	private boolean validateTransactionState = true;
	private ProxyFactory proxyFactory;

	private void initializeProxy() throws Exception {
		if (proxyFactory == null) {
			proxyFactory = new ProxyFactory();
			TransactionInterceptor advice = new TransactionInterceptor(transactionManager,
					PropertiesConverter.stringToProperties(
							"create*=PROPAGATION_REQUIRES_NEW,"
							+ isolationLevelForCreate
							+ "\ngetLastJobExecution*=PROPAGATION_REQUIRES_NEW,"
							+ isolationLevelForCreate
							+ "\n*=PROPAGATION_REQUIRED"));
			if (validateTransactionState) {
				DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(
						new MethodInterceptor() {
					@Override
					public Object invoke(MethodInvocation invocation) throws Throwable {
						if (TransactionSynchronizationManager.isActualTransactionActive()) {
							throw new IllegalStateException(
									"Existing transaction detected in JobRepository. "
											+ "Please fix this and try again (e.g. "
											+ "remove @Transactional annotations from "
											+ "client).");
						}
						return invocation.proceed();
					}
				});
				NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
				pointcut.addMethodName("create*");
				advisor.setPointcut(pointcut);
				proxyFactory.addAdvisor(advisor);
			}
			proxyFactory.addAdvice(advice);
			proxyFactory.setProxyTargetClass(false);
			proxyFactory.addInterface(JobRepository.class);
			proxyFactory.setTarget(getTarget());
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.notNull(transactionManager, "TransactionManager must not be null.");

		initializeProxy();
	}

	private Object getTarget() throws Exception {
		return new JobLaunchingJobRepository(createJobInstanceDao(),
				createJobExecutionDao(), createStepExecutionDao(),
				createExecutionContextDao());
	}

	@Override
	public JobRepository getObject() throws Exception {
		if (proxyFactory == null) {
			afterPropertiesSet();
		}
		return (JobRepository) proxyFactory.getProxy();
	}

	/**
	 * public setter for the isolation level to be used for the transaction when
	 * job execution entities are initially created. The default is
	 * ISOLATION_SERIALIZABLE, which prevents accidental concurrent execution of
	 * the same job (ISOLATION_REPEATABLE_READ would work as well).
	 *
	 * @param isolationLevelForCreate the isolation level name to set
	 *
	 * @see org.springframework.batch.core.repository.support.SimpleJobRepository#createJobExecution(String,
	 * org.springframework.batch.core.JobParameters)
	 */
	public void setIsolationLevelForCreate(String isolationLevelForCreate) {
		this.isolationLevelForCreate = isolationLevelForCreate;
		super.setIsolationLevelForCreate(isolationLevelForCreate);
	}

	/**
	 * Flag to determine whether to check for an existing transaction when a
	 * JobExecution is created. Defaults to true because it is usually a
	 * mistake, and leads to problems with restartability and also to deadlocks
	 * in multi-threaded steps.
	 *
	 * @param validateTransactionState the flag to set
	 */
	public void setValidateTransactionState(boolean validateTransactionState) {
		this.validateTransactionState = validateTransactionState;
		super.setValidateTransactionState(validateTransactionState);
	}

	/**
	 * Public setter for the {@link PlatformTransactionManager}.
	 * @param transactionManager the transactionManager to set
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
		super.setTransactionManager(transactionManager);
	}
}
