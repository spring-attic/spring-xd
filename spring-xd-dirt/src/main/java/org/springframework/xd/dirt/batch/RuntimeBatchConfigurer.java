/*
 * Copyright 2012-2014 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.explore.support.MapJobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring XD runtime specific {@link BatchConfigurer}.
 * Allows {@link JobRepositoryFactory} settings configurable via configuration properties.
 *
 * Basically a modified copy of {@link DefaultBatchConfigurer}
 *
 * @author sshcherbakov
 */
@Component
public class RuntimeBatchConfigurer implements BatchConfigurer {

	private static final Log logger = LogFactory.getLog(RuntimeBatchConfigurer.class);

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

	public static final String DEFAULT_ISOLATION_LEVEL = "ISOLATION_SERIALIZABLE";

	@Autowired(required = false)
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
	public void initialize() throws Exception {
		if (dataSource == null) {
			logger.warn("No datasource was provided...using a Map based JobRepository");

			if (this.transactionManager == null) {
				this.transactionManager = new ResourcelessTransactionManager();
			}

			MapJobRepositoryFactoryBean jobRepositoryFactory = new MapJobRepositoryFactoryBean(this.transactionManager);
			jobRepositoryFactory.afterPropertiesSet();
			this.jobRepository = jobRepositoryFactory.getObject();

			MapJobExplorerFactoryBean jobExplorerFactory = new MapJobExplorerFactoryBean(jobRepositoryFactory);
			jobExplorerFactory.afterPropertiesSet();
			this.jobExplorer = jobExplorerFactory.getObject();
		}
		else {
			this.jobRepository = createJobRepository();

			JobExplorerFactoryBean jobExplorerFactoryBean = new JobExplorerFactoryBean();
			jobExplorerFactoryBean.setDataSource(this.dataSource);
			jobExplorerFactoryBean.afterPropertiesSet();
			this.jobExplorer = jobExplorerFactoryBean.getObject();
		}

		this.jobLauncher = createJobLauncher();
	}

	private JobLauncher createJobLauncher() throws Exception {
		SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
		jobLauncher.setJobRepository(jobRepository);
		jobLauncher.afterPropertiesSet();
		return jobLauncher;
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

	/**
	 * @return
	 */
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
