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

/**
 * @author Glenn Renfro
 * @author Luke Taylor
 *
 */
public class JobDeployer extends AbstractDeployer<JobDefinition> {

	private static final String BEAN_CREATION_EXCEPTION = "org.springframework.beans.factory.BeanCreationException";
	private static final String BEAN_DEFINITION_EXEPTION = "org.springframework.beans.factory.BeanDefinitionStoreException";
	private static final String DEPLOYER_TYPE = "job";
	public JobDeployer(JobDefinitionRepository repository, DeploymentMessageSender messageSender) {
		super(repository, messageSender, DEPLOYER_TYPE);
	}

	@Override
	public void delete(String name) {
		JobDefinition def = getRepository().findOne(name);
		if (def == null) {
			throwNoSuchDefinitionException(name);
		}
		try {
		if (getRepository().exists(name)) {
			undeploy(name);
		}
		getRepository().delete(name);
		} catch (DSLException dslException) {
			getRepository().delete(name);// if it is a DSL exception (meaning
											// bad definition) go ahead a delete
											// the module.
		}
	}

	@Override
	public void deploy(String name) {
		Assert.hasText(name, ErrorMessage.nameEmptyError.getMessage());
		JobDefinition definition = getRepository().findOne(name);
		if (definition == null) {
			throwNoSuchDefinitionException(name);
		}
		List<ModuleDeploymentRequest> requests = parse(name,
				definition.getDefinition());
		try {
			sendDeploymentRequests(name, requests);
		} catch (MessageHandlingException mhe) {
			Throwable cause = mhe.getCause();
			if (cause == null) {
				throw mhe;
			}
			String exceptionClassName = cause.getClass().getName();
			if (exceptionClassName.equals(BEAN_CREATION_EXCEPTION)
					|| exceptionClassName.equals(BEAN_DEFINITION_EXEPTION)) {
				throw new MissingRequiredDefinitionException(
						definition.getName(),
						cause.getMessage());
			} else {
				throw mhe;
			}
		}
	}

	public void undeploy(String name) {
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
			sendDeploymentRequests(name, requests);
		}
		catch (MessageHandlingException ex) {
			// Job is not deployed.
		}
	}
}
