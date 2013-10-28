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
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;

/**
 * @author Glenn Renfro
 * @author Luke Taylor
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 * 
 */
public class JobDeployer extends AbstractInstancePersistingDeployer<JobDefinition, Job> { // extends

	private static final String DEPLOYER_TYPE = "job";

	private static final String JOB_PARAMETERS_KEY = "jobParameters";

	public JobDeployer(DeploymentMessageSender messageSender, JobDefinitionRepository definitionRepository,
			JobRepository instanceRepository,
			XDParser parser) {
		super(definitionRepository, instanceRepository, messageSender, parser, DEPLOYER_TYPE);
	}

	@Override
	protected Job makeInstance(JobDefinition definition) {
		return new Job(definition);
	}

	public void launch(String name, String jobParameters) {
		JobDefinition job = getDefinitionRepository().findOne(name);
		if (job == null) {
			throwNoSuchDefinitionException(name);
		}
		List<ModuleDeploymentRequest> requests = parse(name, job.getDefinition());
		for (ModuleDeploymentRequest request : requests) {
			request.setLaunch(true);
			if (!StringUtils.isEmpty(jobParameters) && jobParameters != null) {
				request.setParameter(JOB_PARAMETERS_KEY, jobParameters);
			}
		}
		try {
			sendDeploymentRequests(name, requests);
		}
		catch (MessageHandlingException ex) {
			// Job is not launched.
		}
	}

}
