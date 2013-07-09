/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.xd.dirt.stream;

import java.util.List;

import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.ResourceDeployer;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;

/**
 * @author Glenn Renfro
 *
 */
public class JobDeployer implements ResourceDeployer<JobDefinition> {
	private final JobDefinitionRepository repository;
	private final JobDeploymentMessageSender messageSender;
	private final StreamParser streamParser = new EnhancedStreamParser();

	public JobDeployer(JobDefinitionRepository repository, JobDeploymentMessageSender messageSender) {
		Assert.notNull(repository, "repository cannot be null");
		Assert.notNull(messageSender, "message sender cannot be null");
		this.repository = repository;
		this.messageSender = messageSender;

	}

	/* (non-Javadoc)
	 * @see org.springframework.xd.dirt.core.ResourceDeployer#deploy(java.lang.String)
	 */
	@Override
	public void deploy(String name) {
		Assert.hasText(name, "name cannot be blank or null");

		JobDefinition jobDefinition = repository.findOne(name);
		Assert.notNull(jobDefinition, "job '" + name+ " not found");
		List<ModuleDeploymentRequest> requests = streamParser.parse(name, jobDefinition.getDefinition());
		messageSender.sendDeploymentRequests(name, requests);
	}

	/* (non-Javadoc)
	 * @see org.springframework.xd.dirt.core.ResourceDeployer#create(java.lang.Object)
	 */
	@Override
	public JobDefinition create(JobDefinition jobDefinition) {
		Assert.notNull(jobDefinition, "Job definition may not be null");
		return repository.save(jobDefinition);
	}

}
