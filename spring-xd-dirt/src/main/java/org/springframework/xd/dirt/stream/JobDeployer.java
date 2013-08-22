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
import org.springframework.util.Assert;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.stream.dsl.DSLException;
import org.springframework.xd.module.ModuleType;

/**
 * @author Glenn Renfro
 * @author Luke Taylor
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 *
 */
public class JobDeployer extends AbstractDeployer<JobDefinition> {

	private static final String BEAN_CREATION_EXCEPTION = "org.springframework.beans.factory.BeanCreationException";

	private static final String BEAN_DEFINITION_EXEPTION = "org.springframework.beans.factory.BeanDefinitionStoreException";

	private static final String DEPLOYER_TYPE = "job";

	private final TriggerDefinitionRepository triggerDefinitionRepository;

	public JobDeployer(JobDefinitionRepository repository, TriggerDefinitionRepository triggerDefinitionRepository,
			DeploymentMessageSender messageSender,
			XDParser parser) {
		super(repository, messageSender, parser, DEPLOYER_TYPE);
		this.triggerDefinitionRepository = triggerDefinitionRepository;
	}

	@Override
	public void delete(String name) {
		JobDefinition def = getDefinitionRepository().findOne(name);
		if (def == null) {
			throwNoSuchDefinitionException(name);
		}
		try {
			if (getDefinitionRepository().exists(name)) {
				undeploy(name);
			}
			getDefinitionRepository().delete(name);
		}
		catch (DSLException dslException) {
			getDefinitionRepository().delete(name);// if it is a DSL exception (meaning
			// bad definition) go ahead a delete
			// the module.
		}
	}

	@Override
	public void deploy(String name) {
		deploy(name, null, null, null, null);
	}

	public void deploy(String name, String jobParameters, String dateFormat, String numberFormat, Boolean makeUnique) {
		Assert.hasText(name, "name cannot be blank or null");
		JobDefinition definition = getDefinitionRepository().findOne(name);
		if (definition == null) {
			throwNoSuchDefinitionException(name);
		}
		List<ModuleDeploymentRequest> requests = parse(name, definition.getDefinition());
		// If the job definition has trigger then, check if the trigger exists
		// TODO: should we do this at the parser?
		// but currently the parser has reference to StreamDefinitionRepository only.
		if (requests != null && requests.get(0).getParameters().containsKey(ModuleType.TRIGGER.getTypeName())) {
			String triggerName = requests.get(0).getParameters().get(ModuleType.TRIGGER.getTypeName());
			if (triggerDefinitionRepository.findOne(triggerName) == null) {
				throwNoSuchDefinitionException(triggerName, ModuleType.TRIGGER.getTypeName());
			}
		}

		for (ModuleDeploymentRequest request : requests) {
			if ("job".equals(request.getType())) {
				if (jobParameters != null) {
					request.setParameter("jobParameters", jobParameters);
				}
				if (dateFormat != null) {
					request.setParameter("dateFormat", dateFormat);
				}
				if (numberFormat != null) {
					request.setParameter("numberFormat", numberFormat);
				}
				if (makeUnique != null) {
					request.setParameter("makeUnique", String.valueOf(makeUnique));
				}
			}
		}

		try {
			sendDeploymentRequests(name, requests);
		}
		catch (MessageHandlingException mhe) {
			Throwable cause = mhe.getCause();
			if (cause == null) {
				throw mhe;
			}
			String exceptionClassName = cause.getClass().getName();
			if (exceptionClassName.equals(BEAN_CREATION_EXCEPTION)
					|| exceptionClassName.equals(BEAN_DEFINITION_EXEPTION)) {
				throw new MissingRequiredDefinitionException(definition.getName(), cause.getMessage());
			}
			else {
				throw mhe;
			}
		}
	}

	@Override
	public void undeploy(String name) {
		JobDefinition job = getDefinitionRepository().findOne(name);
		if (job == null) {
			throwNoSuchDefinitionException(name);
		}
		List<ModuleDeploymentRequest> requests = parse(name, job.getDefinition());
		for (ModuleDeploymentRequest request : requests) {
			request.setRemove(true);
		}
		try {
			sendDeploymentRequests(name, requests);
		}
		catch (MessageHandlingException ex) {
			// Job is not deployed.
		}
	}

	@Override
	public void deployAll() {
		// TODO: we should revisit here to check on instance repository
		for (JobDefinition definition : getDefinitionRepository().findAll()) {
			deploy(definition.getName());
		}
	}

	@Override
	public void undeployAll() {
		// TODO: we should revisit here to check on instance repository
		for (JobDefinition definition : getDefinitionRepository().findAll()) {
			undeploy(definition.getName());
		}
	}

	@Override
	public void deleteAll() {
		undeployAll();
		super.deleteAll();
	}
}
