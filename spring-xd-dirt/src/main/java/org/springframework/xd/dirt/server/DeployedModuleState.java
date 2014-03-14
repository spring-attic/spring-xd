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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationListener;
import org.springframework.xd.dirt.event.AbstractModuleEvent;
import org.springframework.xd.module.core.Module;


/**
 * Listens for {@link AbstractModuleEvent} and tracks the state of module deployment.
 * 
 * @author David Turanski
 */
public class DeployedModuleState implements ApplicationListener<AbstractModuleEvent> {

	private static Logger log = LoggerFactory.getLogger(DeployedModuleState.class);

	private final ConcurrentMap<String, Map<Integer, Module>> deployedModules = new ConcurrentHashMap<String, Map<Integer, Module>>();

	@Override
	public void onApplicationEvent(AbstractModuleEvent event) {
		log.info("got event " + event.getType() + "module:" + event.getSource());
		Module module = event.getSource();
		if (event.getType().equals("ModuleDeployed")) {
			this.deployedModules.putIfAbsent(module.getDeploymentMetadata().getGroup(),
					new HashMap<Integer, Module>());
			this.deployedModules.get(module.getDeploymentMetadata().getGroup()).put(
					module.getDeploymentMetadata().getIndex(), module);
		}
		else {
			this.deployedModules.get(module.getDeploymentMetadata().getGroup()).remove(
					module.getDeploymentMetadata().getIndex());
		}
	}

	public Map<String, Map<Integer, Module>> getDeployedModules() {
		return this.deployedModules;
	}
}
