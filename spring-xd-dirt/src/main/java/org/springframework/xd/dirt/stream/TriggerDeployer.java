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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.module.ModuleRegistry;

/**
 * Responsible for deploying {@link TriggerDefinition}s.
 * 
 * @author Gunnar Hillert
 * @author Luke Taylor
 * @author Glenn Renfro
 * @since 1.0
 * 
 */
public class TriggerDeployer extends AbstractDeployer<TriggerDefinition> {

	@Autowired
	private ModuleRegistry moduleRegistry;

	public TriggerDeployer(TriggerDefinitionRepository repository, DeploymentMessageSender messageSender,
			XDParser parser) {
		super(repository, messageSender, parser, "trigger");
	}

	@Override
	public void delete(String name) {
		TriggerDefinition def = getRepository().findOne(name);
		if (def == null) {
			throw new NoSuchDefinitionException(name, "Can't delete trigger '%s' because it does not exist");
		}
		undeploy(name);
		getRepository().delete(name);
	}

	@Override
	public void undeploy(String name) {
		TriggerDefinition trigger = getRepository().findOne(name);
		if (trigger == null) {
			throwNoSuchDefinitionException(name);
		}
		List<ModuleDeploymentRequest> requests = parse(name, trigger.getDefinition());
		for (ModuleDeploymentRequest request : requests) {
			request.setRemove(true);
		}
		sendDeploymentRequests(name, requests);
	}

}
