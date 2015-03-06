/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.xd.dirt.core;

import java.util.Map;

import org.springframework.xd.dirt.stream.AlreadyDeployedException;
import org.springframework.xd.dirt.stream.DefinitionAlreadyExistsException;
import org.springframework.xd.dirt.stream.NoSuchDefinitionException;
import org.springframework.xd.dirt.stream.NotDeployedException;

/**
 * This interface sets up validation methods that need to be implemented before producing the
 * {@link org.springframework.xd.dirt.server.admin.deployment.DeploymentMessage}s.
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
