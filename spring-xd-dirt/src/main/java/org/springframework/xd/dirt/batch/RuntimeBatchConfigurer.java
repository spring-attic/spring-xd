/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.xd.dirt.batch;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.plugins.job.support.JobLaunchingJobRepositoryFactoryBean;

/**
 * Spring XD runtime specific {@link BatchConfigurer}.
 * Allows {@link JobRepositoryFactoryBean} settings configurable via configuration properties.
 *
 * Basically a modified copy of {@link DefaultBatchConfigurer}
 *
 * @author sshcherbakov
 */
@Component
public class RuntimeBatchConfigurer implements BatchConfigurer {

	private DataSource dataSource;

	private PlatformTransactionManager transactionManager;

	private JobRepository jobRepository;

	private JobLauncher jobLauncher;

	private JobExplorer jobExplorer;

	private String isolationLevel = DEFAULT_ISOLATION_LEVEL;

	private Integer clobType = null;

	private String dbType = null;

	private int maxVarCharLength = AbstractJdbcBatchMetadataDao.DEFAULT_EXIT_MESSAGE_LENGTH;

	private String tablePrefix = AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX;

	private boolean validateTransactionState = true;

	public static final String DEFAULT_ISOLATION_LEVEL = "ISOLATION_READ_COMMITTED";

	@Autowired(required = true)
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		this.transactionManager = new DataSourceTransactionManager(dataSource);
	}

	protected RuntimeBatchConfigurer() {
	}

	public RuntimeBatchConfigurer(DataSource dataSource) {
		setDataSource(dataSource);
	}

	@Override
	public JobRepository getJobRepository() {
		return jobRepository;
	}

	@Override
	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	@Override
	public JobLauncher getJobLauncher() {
		return jobLauncher;
	}

	@Override
	public JobExplorer getJobExplorer() {
		return jobExplorer;
	}

	@PostConstruct
	public void initialize() {
		Assert.notNull(dataSource, "No dataSource was provided for Batch runtime configuration");
		try {
			this.jobRepository = createJobRepository();
			this.jobExplorer = createJobExplorer();
			this.jobLauncher = createJobLauncher();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to initialize Spring Batch", ex);
		}
	}

	private JobExplorer createJobExplorer() throws Exception {
		JobExplorerFactoryBean jobExplorerFactoryBean = new JobExplorerFactoryBean();
		jobExplorerFactoryBean.setDataSource(this.dataSource);
		jobExplorerFactoryBean.afterPropertiesSet();
		return jobExplorerFactoryBean.getObject();
	}

	private JobLauncher createJobLauncher() throws Exception {
		SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
		jobLauncher.setJobRepository(createLaunchingJobRepository());
		jobLauncher.afterPropertiesSet();
		return jobLauncher;
	}

	private JobRepository createLaunchingJobRepository() throws Exception {
		JobLaunchingJobRepositoryFactoryBean factory = new JobLaunchingJobRepositoryFactoryBean();
		factory.setDataSource(dataSource);
		if (dbType != null) {
			factory.setDatabaseType(dbType);
		}
		if (clobType != null) {
			factory.setClobType(clobType);
		}
		factory.setTransactionManager(transactionManager);
		factory.setIsolationLevelForCreate(isolationLevel);
		factory.setMaxVarCharLength(maxVarCharLength);
		factory.setTablePrefix(tablePrefix);
		factory.setValidateTransactionState(validateTransactionState);
		factory.afterPropertiesSet();

		return factory.getObject();
	}

	protected JobRepository createJobRepository() throws Exception {
		JobRepositoryFactoryBean factory = createJobRepositoryFactoryBean();
		factory.setDataSource(dataSource);
		if (dbType != null) {
			factory.setDatabaseType(dbType);
		}
		if (clobType != null) {
			factory.setClobType(clobType);
		}
		factory.setTransactionManager(transactionManager);
		factory.setIsolationLevelForCreate(isolationLevel);
		factory.setMaxVarCharLength(maxVarCharLength);
		factory.setTablePrefix(tablePrefix);
		factory.setValidateTransactionState(validateTransactionState);
		factory.afterPropertiesSet();
		return factory.getObject();
	}

	JobRepositoryFactoryBean createJobRepositoryFactoryBean() {
		return new JobRepositoryFactoryBean();
	}


	public void setIsolationLevel(String isolationLevel) {
		this.isolationLevel = isolationLevel;
	}

	public void setClobType(Integer clobType) {
		this.clobType = clobType;
	}


	public void setDbType(String dbType) {
		this.dbType = dbType;
	}


	public void setMaxVarCharLength(int maxVarCharLength) {
		this.maxVarCharLength = maxVarCharLength;
	}


	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}


	public void setValidateTransactionState(boolean validateTransactionState) {
		this.validateTransactionState = validateTransactionState;
	}

}
