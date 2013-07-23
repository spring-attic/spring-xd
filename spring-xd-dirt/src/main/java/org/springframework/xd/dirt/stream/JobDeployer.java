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

import org.springframework.integration.MessageHandlingException;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;

/**
 * @author Glenn Renfro
 * @author Luke Taylor
 *
 */
public class JobDeployer extends AbstractDeployer<JobDefinition> {

	public JobDeployer(JobDefinitionRepository repository, DeploymentMessageSender messageSender) {
		super(repository, messageSender, "job");
	}

	@Override
	public void delete(String name) {
		JobDefinition def = getRepository().findOne(name);
		if (def == null) {
			throwNoSuchDefinitionException(name);
		}
		if (getRepository().exists(name)) {
			undeployJob(name);
		}

		getRepository().delete(name);
	}

	public void undeployJob(String name) {
		JobDefinition job = getRepository().findOne(name);
		if (job == null) {
			throwNoSuchDefinitionException(name);
		}
		StreamParser streamParser = new EnhancedStreamParser();
		List<ModuleDeploymentRequest> requests = streamParser.parse(name, job.getDefinition());
		for (ModuleDeploymentRequest request : requests) {
			request.setRemove(true);
		}
		try {
			getMessageSender().sendDeploymentRequests(name, requests);
		}
		catch (MessageHandlingException ex) {
			// Job is not deployed.
		}

	}
}
