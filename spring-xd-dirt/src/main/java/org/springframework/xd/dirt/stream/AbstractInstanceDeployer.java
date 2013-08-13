/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.xd.dirt.core.BaseDefinition;

/**
 * Base support class for deployers that know how to deal with {@link BaseInstance instances} of a
 * {@link BaseDefinition definition}.
 * 
 * @author Eric Bottard
 */
public class AbstractInstanceDeployer<D extends BaseDefinition, I extends BaseInstance<D>> extends AbstractDeployer<D> {

	protected AbstractInstanceDeployer(PagingAndSortingRepository<D, String> repository,
			DeploymentMessageSender messageSender, XDParser parser, String definitionKind) {
		super(repository, messageSender, parser, definitionKind);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void delete(String name) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public void undeploy(String name) {

	}

	@Override
	public void deploy(String name) {
		// Assert.hasText(name, ErrorMessage.nameEmptyError.getMessage());
		//
		// D definition = repository.findOne(name);
		//
		// if (definition == null) {
		// throwNoSuchDefinitionException(name);
		// }
		// List<ModuleDeploymentRequest> requests = streamParser.parse(name, definition.getDefinition());
		// messageSender.sendDeploymentRequests(name, requests);
	}

}
