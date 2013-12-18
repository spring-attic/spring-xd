/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.rest;

import static org.mockito.Mockito.mock;

import org.springframework.batch.admin.service.JdbcSearchableJobExecutionDao;
import org.springframework.batch.admin.service.JdbcSearchableJobInstanceDao;
import org.springframework.batch.admin.service.JdbcSearchableStepExecutionDao;
import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.admin.service.SearchableJobExecutionDao;
import org.springframework.batch.admin.service.SearchableJobInstanceDao;
import org.springframework.batch.admin.service.SearchableStepExecutionDao;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JdbcExecutionContextDao;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.xd.analytics.metrics.core.AggregateCounterRepository;
import org.springframework.xd.analytics.metrics.core.CounterRepository;
import org.springframework.xd.analytics.metrics.core.FieldValueCounterRepository;
import org.springframework.xd.analytics.metrics.core.GaugeRepository;
import org.springframework.xd.analytics.metrics.core.RichGaugeRepository;
import org.springframework.xd.dirt.container.store.RuntimeContainerInfoRepository;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDependencyRepository;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.module.memory.InMemoryModuleDefinitionRepository;
import org.springframework.xd.dirt.module.memory.InMemoryModuleDependencyRepository;
import org.springframework.xd.dirt.module.store.RuntimeContainerModuleInfoRepository;
import org.springframework.xd.dirt.module.store.RuntimeModuleInfoRepository;
import org.springframework.xd.dirt.plugins.job.BatchJobLocator;
import org.springframework.xd.dirt.plugins.job.DistributedJobService;
import org.springframework.xd.dirt.stream.CompositeModuleDefinitionService;
import org.springframework.xd.dirt.stream.DeploymentMessageSender;
import org.springframework.xd.dirt.stream.JobDefinitionRepository;
import org.springframework.xd.dirt.stream.JobDeployer;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamDeployer;
import org.springframework.xd.dirt.stream.StreamRepository;
import org.springframework.xd.dirt.stream.XDStreamParser;
import org.springframework.xd.dirt.stream.memory.InMemoryJobDefinitionRepository;
import org.springframework.xd.dirt.stream.memory.InMemoryJobRepository;
import org.springframework.xd.dirt.stream.memory.InMemoryStreamDefinitionRepository;
import org.springframework.xd.dirt.stream.memory.InMemoryStreamRepository;
import org.springframework.xd.module.options.DefaultModuleOptionsMetadataResolver;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;

/**
 * Provide a mockito mock for any of the business layer dependencies. Adding yet another configuration class on top, one
 * can selectively override those mocks (with <i>e.g.</i> in memory implementations).
 * 
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
@Configuration
public class Dependencies {

	@Bean
	@Qualifier("simple")
	public CounterRepository counterRepository() {
		return mock(CounterRepository.class);
	}

	@Bean
	public AggregateCounterRepository aggregateCounterRepository() {
		return mock(AggregateCounterRepository.class);
	}

	@Bean
	public GaugeRepository gaugeRepository() {
		return mock(GaugeRepository.class);
	}

	@Bean
	public ModuleDefinitionRepository moduleDefinitionRepository() {
		return new InMemoryModuleDefinitionRepository(moduleRegistry(), moduleDependencyRepository());
	}

	@Bean
	public ModuleRegistry moduleRegistry() {
		return mock(ModuleRegistry.class);
	}

	@Bean
	public ModuleOptionsMetadataResolver moduleOptionsMetadataResolver() {
		return new DefaultModuleOptionsMetadataResolver();
	}

	@Bean
	public RichGaugeRepository richGaugeRepository() {
		return mock(RichGaugeRepository.class);
	}

	@Bean
	public FieldValueCounterRepository fieldValueCounterRepository() {
		return mock(FieldValueCounterRepository.class);
	}

	@Bean
	public DeploymentMessageSender deploymentMessageSender() {
		return mock(DeploymentMessageSender.class);
	}

	@Bean
	public XDStreamParser parser() {
		return new XDStreamParser(streamDefinitionRepository(),
				moduleDefinitionRepository(), moduleOptionsMetadataResolver());
	}

	@Bean
	public JobDefinitionRepository jobDefinitionRepository() {
		return new InMemoryJobDefinitionRepository();
	}

	@Bean
	public JobDeployer jobDeployer() {
		return new JobDeployer(deploymentMessageSender(), jobDefinitionRepository(), xdJobRepository(), parser());
	}

	@Bean
	public StreamDefinitionRepository streamDefinitionRepository() {
		return new InMemoryStreamDefinitionRepository(moduleDependencyRepository());
	}

	@Bean
	public ModuleDependencyRepository moduleDependencyRepository() {
		return new InMemoryModuleDependencyRepository();
	}

	@Bean
	public CompositeModuleDefinitionService compositeModuleDefinitionService() {
		return new CompositeModuleDefinitionService(moduleDefinitionRepository(), parser());
	}

	@Bean
	public StreamDeployer streamDeployer() {
		return new StreamDeployer(streamDefinitionRepository(), deploymentMessageSender(), streamRepository(),
				parser());
	}

	@Bean
	public StreamRepository streamRepository() {
		return new InMemoryStreamRepository();
	}

	@Bean
	public org.springframework.xd.dirt.stream.JobRepository xdJobRepository() {
		return new InMemoryJobRepository();
	}

	@Bean
	public RuntimeContainerInfoRepository containerRepository() {
		return mock(RuntimeContainerInfoRepository.class);
	}

	@Bean
	public RuntimeModuleInfoRepository modulesRepository() {
		return mock(RuntimeModuleInfoRepository.class);
	}

	@Bean
	public RuntimeContainerModuleInfoRepository containerModulesRepository() {
		return mock(RuntimeContainerModuleInfoRepository.class);
	}

	@Bean
	public JobService jobService() {
		return mock(DistributedJobService.class);
	}

	@Bean
	public SearchableJobInstanceDao searchableJobInstanceDao() {
		return mock(JdbcSearchableJobInstanceDao.class);
	}

	@Bean
	public SearchableJobExecutionDao searchableJobExecutionDao() {
		return mock(JdbcSearchableJobExecutionDao.class);
	}

	@Bean
	public SearchableStepExecutionDao searchableStepExecutionDao() {
		return mock(JdbcSearchableStepExecutionDao.class);
	}

	@Bean
	public JobRepository jobRepository() {
		return mock(JobRepository.class);
	}

	@Bean
	public JobLauncher jobLauncher() {
		return mock(SimpleJobLauncher.class);
	}

	@Bean
	public BatchJobLocator batchJobLocator() {
		return mock(BatchJobLocator.class);
	}

	@Bean
	public ExecutionContextDao executionContextDao() {
		return mock(JdbcExecutionContextDao.class);
	}
}
