/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.xd.dirt.stream;

import java.util.Map;

/**
 * This interface defines validation methods that verify a stream or job
 * can be saved, deployed, undeployed, or deleted. Successful invocation
 * of these methods indicates that the operation may proceed, whereas
 * an exception indicates that the operation will not succeed.
 * <p>
 * This mechanism is useful when issuing operations asynchronously, as
 * is the case when deploying via
 * {@link org.springframework.xd.dirt.server.admin.deployment.DeploymentMessage} and
 * {@link org.springframework.xd.dirt.server.admin.deployment.DeploymentMessagePublisher}.
 *
 * @author Ilayaperumal Gopinathan
 */
public interface DeploymentValidator {

	/**
	 * Validate before saving the definitions.
	 *
	 * @param name the deployment unit name
	 * @param definition the definition
	 * @throws DefinitionAlreadyExistsException
	 */
	void validateBeforeSave(String name, String definition) throws DefinitionAlreadyExistsException;

	/**
	 * Validate before deploying.
	 *
	 * @param name the deployment unit name
	 * @param properties
	 * @throws AlreadyDeployedException
	 * @throws DefinitionAlreadyExistsException
	 */
	void validateBeforeDeploy(String name, Map<String, String> properties) throws AlreadyDeployedException,
			DefinitionAlreadyExistsException;

	/**
	 * Validate before un-deployment.
	 *
	 * @param name the deployment unit name
	 * @throws NoSuchDefinitionException
	 * @throws NotDeployedException
	 */
	void validateBeforeUndeploy(String name) throws NoSuchDefinitionException, NotDeployedException;

	/**
	 * Validate before deletion of the definition.
	 *
	 * @param name the deployment unit name
	 * @throws NoSuchDefinitionException
	 */
	void validateBeforeDelete(String name) throws NoSuchDefinitionException;
}
