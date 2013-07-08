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
 * Responsible for deploying {@link TriggerDefinition}s.
 *
 * @author Gunnar Hillert
 * @since 1.0
 *
 */
public class TriggerDeployer implements ResourceDeployer<TriggerDefinition> {

	private final TriggerDefinitionRepository repository;
	private final TriggerDeploymentMessageSender messageSender;

	//TODO This should possibly be handled by a dedicated TriggerStreamParser
	private final StreamParser streamParser = new EnhancedStreamParser();

	public TriggerDeployer(TriggerDefinitionRepository repository, TriggerDeploymentMessageSender messageSender) {
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

		TriggerDefinition triggerDefinition = repository.findOne(name);
		Assert.notNull(triggerDefinition, "tap '" + name+ " not found");
		List<ModuleDeploymentRequest> requests = streamParser.parse(name, triggerDefinition.getDefinition());
		messageSender.sendDeploymentRequests(name, requests);
	}

	/* (non-Javadoc)
	 * @see org.springframework.xd.dirt.core.ResourceDeployer#create(java.lang.Object)
	 */
	@Override
	public TriggerDefinition create(TriggerDefinition triggerDefinition) {
		Assert.notNull(triggerDefinition, "trigger definition may not be null");
		return repository.save(triggerDefinition);
	}

}
