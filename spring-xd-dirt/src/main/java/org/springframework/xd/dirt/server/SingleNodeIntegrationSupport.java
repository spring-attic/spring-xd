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

package org.springframework.xd.dirt.server;

import java.util.Map;
import java.util.Map.Entry;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamDeployer;
import org.springframework.xd.dirt.stream.StreamRepository;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.core.Module;


/**
 * @author David Turanski
 * 
 */
abstract class SingleNodeIntegrationSupport {

	private StreamDefinitionRepository streamDefinitionRepository;

	private StreamRepository streamRepository;

	private StreamDeployer streamDeployer;

	private DeployedModuleState deployedModuleState;

	protected ConfigurableApplicationContext adminContext;

	protected ConfigurableApplicationContext containerContext;

	protected void init() {
		deployedModuleState = new DeployedModuleState();
		streamDefinitionRepository = containerContext().getBean(StreamDefinitionRepository.class);
		streamRepository = containerContext().getBean(StreamRepository.class);
		streamDeployer = adminContext().getBean(StreamDeployer.class);
		containerContext().addApplicationListener(deployedModuleState);
	}

	public ConfigurableApplicationContext adminContext() {
		return adminContext;
	}

	public ConfigurableApplicationContext containerContext() {
		return containerContext;
	}

	public final StreamDeployer streamDeployer() {
		return streamDeployer;
	}

	public final StreamRepository streamRepository() {
		return streamRepository;
	}

	public final StreamDefinitionRepository streamDefinitionRepository() {
		return streamDefinitionRepository;
	}

	public final MessageBus messageBus() {
		return deployedModuleState.getMessageBus();
	}

	public final Map<String, Map<Integer, Module>> deployedModules() {
		return deployedModuleState.getDeployedModules();
	}


	public final boolean deployStream(StreamDefinition definition) {
		return waitForDeploy(definition);
	}


	public final boolean createAndDeployStream(StreamDefinition definition) {
		streamDeployer.save(definition);
		return waitForDeploy(definition);
	}

	public final boolean undeployStream(StreamDefinition definition) {
		return waitForUndeploy(definition);
	}

	public final boolean undeployAndDestroyStream(StreamDefinition definition) {
		boolean result = waitForUndeploy(definition);
		streamDeployer.delete(definition.getName());
		return result;
	}

	public final Module getModule(String moduleName, int index) {
		final Map<String, Map<Integer, Module>> deployedModules = deployedModuleState.getDeployedModules();

		Module matchedModule = null;
		for (Entry<String, Map<Integer, Module>> entry : deployedModules.entrySet()) {
			final Module module = entry.getValue().get(index);
			if (module != null && moduleName.equals(module.getName())) {
				matchedModule = module;
				break;
			}
		}
		return matchedModule;
	}

	private final boolean waitForStreamOp(StreamDefinition definition, boolean isDeploy) {
		final int MAX_TRIES = 40;
		int tries = 1;
		boolean done = false;
		while (!done && tries <= MAX_TRIES) {
			done = true;
			int i = definition.getModuleDefinitions().size();
			for (ModuleDefinition module : definition.getModuleDefinitions()) {
				Module deployedModule = getModule(module.getName(), --i);

				done = (isDeploy) ? deployedModule != null : deployedModule == null;
				if (!done) {
					break;
				}
			}
			if (!done) {
				try {
					Thread.sleep(100);
					tries++;
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
		return done;
	}

	private final boolean waitForUndeploy(StreamDefinition definition) {
		streamDeployer.undeploy(definition.getName());
		return waitForStreamOp(definition, false);
	}

	private final boolean waitForDeploy(StreamDefinition definition) {
		streamDeployer.deploy(definition.getName());
		return waitForStreamOp(definition, true);
	}

}
