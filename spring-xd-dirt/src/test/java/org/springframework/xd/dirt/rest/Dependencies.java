/*
 * Copyright 2013-2015 the original author or authors.
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

import org.springframework.batch.admin.service.JdbcSearchableJobInstanceDao;
import org.springframework.batch.admin.service.JdbcSearchableStepExecutionDao;
import org.springframework.batch.admin.service.SearchableJobInstanceDao;
import org.springframework.batch.admin.service.SearchableStepExecutionDao;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JdbcExecutionContextDao;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.xd.analytics.metrics.core.AggregateCounterRepository;
import org.springframework.xd.analytics.metrics.core.CounterRepository;
import org.springframework.xd.analytics.metrics.core.FieldValueCounterRepository;
import org.springframework.xd.analytics.metrics.core.GaugeRepository;
import org.springframework.xd.analytics.metrics.core.RichGaugeRepository;
import org.springframework.xd.dirt.container.store.ContainerRepository;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.integration.bus.local.LocalMessageBus;
import org.springframework.xd.dirt.job.dao.XdJdbcSearchableJobExecutionDao;
import org.springframework.xd.dirt.module.ModuleDependencyRepository;
import org.springframework.xd.dirt.module.WritableModuleRegistry;
import org.springframework.xd.dirt.module.store.ModuleMetadataRepository;
import org.springframework.xd.dirt.module.store.ZooKeeperModuleDependencyRepository;
import org.springframework.xd.dirt.module.support.ModuleDefinitionService;
import org.springframework.xd.dirt.plugins.job.DistributedJobLocator;
import org.springframework.xd.dirt.plugins.job.DistributedJobService;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentHandler;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentMessage;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentMessagePublisher;
import org.springframework.xd.dirt.server.admin.deployment.zk.DeploymentMessageConsumer;
import org.springframework.xd.dirt.stream.JobDefinitionRepository;
import org.springframework.xd.dirt.stream.JobDeployer;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamDeployer;
import org.springframework.xd.dirt.stream.StreamRepository;
import org.springframework.xd.dirt.stream.XDStreamParser;
import org.springframework.xd.dirt.stream.zookeeper.ZooKeeperJobDefinitionRepository;
import org.springframework.xd.dirt.stream.zookeeper.ZooKeeperJobRepository;
import org.springframework.xd.dirt.stream.zookeeper.ZooKeeperStreamDefinitionRepository;
import org.springframework.xd.dirt.stream.zookeeper.ZooKeeperStreamRepository;
import org.springframework.xd.dirt.zookeeper.EmbeddedZooKeeper;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.module.info.DefaultSimpleModuleInformationResolver;
import org.springframework.xd.module.info.ModuleInformationResolver;
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
@EnableIntegration
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
	public WritableModuleRegistry moduleRegistry() {
		return mock(WritableModuleRegistry.class);
	}

	@Bean
	public ModuleOptionsMetadataResolver moduleOptionsMetadataResolver() {
		return new DefaultModuleOptionsMetadataResolver();
	}

	@Bean
	public ModuleInformationResolver moduleInformationResolver() {
		return new DefaultSimpleModuleInformationResolver();
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
	public ThreadPoolTaskScheduler taskScheduler() {
		return new ThreadPoolTaskScheduler();
	}

	@Bean
	public MessageBus messageBus() {
		PollerMetadata poller = new PollerMetadata();
		poller.setTaskExecutor(taskScheduler());
		LocalMessageBus bus = new LocalMessageBus();
		bus.setPoller(poller);
		return bus;
	}

	@Bean
	public XDStreamParser parser() {
		return new XDStreamParser(streamDefinitionRepository(),
				moduleRegistry(), moduleOptionsMetadataResolver());
	}

	@Bean
	public JobDefinitionRepository jobDefinitionRepository() {
		return new ZooKeeperJobDefinitionRepository(zooKeeperConnection());
	}

	@Bean
	public JobDeployer jobDeployer() {
		return new JobDeployer(zooKeeperConnection(), jobDefinitionRepository(), xdJobRepository(), parser(),
				messageBus(), deploymentHandler());
	}

	@Bean
	public EmbeddedZooKeeper embeddedZooKeeper() {
		return new EmbeddedZooKeeper();
	}

	@Bean
	public ZooKeeperConnection zooKeeperConnection() {
		ZooKeeperConnection zkc = new ZooKeeperConnection("localhost:" + embeddedZooKeeper().getClientPort());
		zkc.setAutoStartup(true);
		return zkc;
	}

	@Bean
	public StreamDefinitionRepository streamDefinitionRepository() {
		return new ZooKeeperStreamDefinitionRepository(zooKeeperConnection(), moduleDependencyRepository());
	}

	@Bean
	public ModuleDependencyRepository moduleDependencyRepository() {
		return new ZooKeeperModuleDependencyRepository(zooKeeperConnection());
	}

	@Bean
	public ModuleDefinitionService moduleDefinitionService() {
		return new ModuleDefinitionService(moduleRegistry(), parser(), moduleDependencyRepository());
	}

	@Bean
	public StreamDeployer streamDeployer() {
		return new StreamDeployer(zooKeeperConnection(), streamDefinitionRepository(), streamRepository(), parser(),
				deploymentHandler());
	}

	@Bean
	public StreamRepository streamRepository() {
		return new ZooKeeperStreamRepository(zooKeeperConnection());
	}

	@Bean
	public org.springframework.xd.dirt.stream.JobRepository xdJobRepository() {
		return new ZooKeeperJobRepository(zooKeeperConnection());
	}

	@Bean
	public ContainerRepository containerRepository() {
		return mock(ContainerRepository.class);
	}

	@Bean
	public ModuleMetadataRepository modulesRepository() {
		return mock(ModuleMetadataRepository.class);
	}

	@Bean
	public DistributedJobService jobService() {
		return mock(DistributedJobService.class);
	}

	@Bean
	public SearchableJobInstanceDao searchableJobInstanceDao() {
		return mock(JdbcSearchableJobInstanceDao.class);
	}

	@Bean
	public XdJdbcSearchableJobExecutionDao searchableJobExecutionDao() {
		return mock(XdJdbcSearchableJobExecutionDao.class);
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
	public DistributedJobLocator distributedJobLocator() {
		return mock(DistributedJobLocator.class);
	}

	@Bean
	public JobRegistry jobRegistry() {
		return mock(JobRegistry.class);
	}

	@Bean
	public ExecutionContextDao executionContextDao() {
		return mock(JdbcExecutionContextDao.class);
	}

	@Bean
	public DeploymentMessagePublisher DeploymentMessageProducer() {
		return new DeploymentMessagePublisher() {

			DeploymentMessageConsumer consumer = new DeploymentMessageConsumer();

			@Override
			public void publish(DeploymentMessage deploymentMessage) {
				try {
					consumer.consumeMessage(deploymentMessage, streamDeployer(), jobDeployer());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public void poll(DeploymentMessage message) {
				// todo: the "real" implementation of poll waits until
				// the deployment message has been processed; this
				// should be revisited to see if/how the same behavior
				// can be mimicked
				publish(message);
			}
		};
	}

	@Bean
	public DeploymentHandler deploymentHandler() {
		return new DeploymentHandler() {

			@Override
			public void deploy(String deploymentUnitName) throws Exception {

			}

			@Override
			public void undeploy(String deploymentUnitName) throws Exception {

			}
		};
	}
}
