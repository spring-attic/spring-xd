/*
 * Copyright 2002-2014 the original author or authors.
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

import static org.springframework.xd.dirt.stream.ParsingContext.job;

import java.util.List;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;

/**
 * @author Glenn Renfro
 * @author Luke Taylor
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 */
public class JobDeployer extends AbstractInstancePersistingDeployer<JobDefinition, Job> { // extends

	private static final String JOB_PARAMETERS_KEY = "jobParameters";

	// todo: get rid of this
	private final DeploymentMessageSender messageSender;

	public JobDeployer(JobDefinitionRepository definitionRepository,
			JobRepository instanceRepository,
			XDParser parser,
			DeploymentMessageSender messageSender) {
		super(definitionRepository, instanceRepository, parser, job);
		Assert.notNull(messageSender, "Message sender cannot be null");
		this.messageSender = messageSender;
	}

	@Override
	protected Job makeInstance(JobDefinition definition) {
		return new Job(definition);
	}

	// todo: replace this with use of the MessageBus, sending to queue:job:jobname
	public void launch(String name, String jobParameters) {
		// Double check so that user gets an informative error message
		JobDefinition job = getDefinitionRepository().findOne(name);
		if (job == null) {
			throwNoSuchDefinitionException(name);
		}
		Job instance = instanceRepository.findOne(name);
		if (instance == null) {
			throwNotDeployedException(name);
		}
		List<ModuleDeploymentRequest> requests = parse(name, job.getDefinition());
		Assert.isTrue(requests.size() == 1, "Expecting only a single module");
		ModuleDeploymentRequest request = requests.get(0);
		request.setLaunch(true);
		if (!StringUtils.isEmpty(jobParameters)) {
			request.setParameter(JOB_PARAMETERS_KEY, jobParameters);
		}
		messageSender.sendDeploymentRequests(name, requests);
	}

	@Override
	protected JobDefinition createDefinition(String name, String definition, boolean deploy) {
		return new JobDefinition(name, definition, deploy);
	}

	// TODO: The ModuleDefinition currently does not provide sourceChannelName and sinkChannelName required for
	// deployment. This is only provided by the parser
	private List<ModuleDeploymentRequest> parse(String name, String definition) {
		return this.streamParser.parse(name, definition, definitionKind);
	}

}
