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

package org.springframework.xd.dirt.integration.test;

import java.util.HashMap;
import java.util.Map;

import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;

import org.springframework.integration.x.bus.MessageBus;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.module.DelegatingModuleRegistry;
import org.springframework.xd.dirt.module.ModuleDeployer;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.module.ResourceModuleRegistry;
import org.springframework.xd.dirt.server.SingleNodeApplication;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamDeployer;
import org.springframework.xd.dirt.stream.StreamRepository;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.core.Module;


/**
 * A helper class that provides methods used for testing streams with {@link SingleNodeApplication}. It exposes
 * components and methods used for stream creation, deployment, and destruction and provides access to the
 * {@link MessageBus}. Additionally, it supports registration of modules contained in a local resource location
 * (default: "file:./config").
 * 
 * 
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 * 
 */
public class SingleNodeIntegrationTestSupport {

	private StreamDefinitionRepository streamDefinitionRepository;

	private StreamRepository streamRepository;

	private StreamDeployer streamDeployer;

	private MessageBus messageBus;

	private final ModuleDeployer moduleDeployer;

	private ZooKeeperConnection zooKeeperConnection;

	private final Map<String, PathChildrenCache> mapChildren = new HashMap<String, PathChildrenCache>();

	public SingleNodeIntegrationTestSupport(SingleNodeApplication application) {
		this(application, "file:./config");
	}

	/**
	 * Constructor useful for testing custom modules
	 * 
	 * @param application the {@link SingleNodeApplication}
	 * @param moduleResourceLocation an additional Spring (file: or classpath:) resource location used by the
	 *        {@link ModuleRegistry}
	 */
	public SingleNodeIntegrationTestSupport(SingleNodeApplication application, String moduleResourceLocation) {
		Assert.notNull(application, "SingleNodeApplication must not be null");
		streamDefinitionRepository = application.pluginContext().getBean(StreamDefinitionRepository.class);
		streamRepository = application.pluginContext().getBean(StreamRepository.class);
		streamDeployer = application.adminContext().getBean(StreamDeployer.class);
		messageBus = application.pluginContext().getBean(MessageBus.class);
		zooKeeperConnection = application.adminContext().getBean(ZooKeeperConnection.class);
		moduleDeployer = application.containerContext().getBean(ModuleDeployer.class);
		ResourceModuleRegistry cp = new ResourceModuleRegistry(moduleResourceLocation);
		DelegatingModuleRegistry cmr1 = application.pluginContext().getBean(DelegatingModuleRegistry.class);
		cmr1.addDelegate(cp);
		DelegatingModuleRegistry cmr2 = application.adminContext().getBean(DelegatingModuleRegistry.class);
		if (cmr1 != cmr2) {
			cmr2.addDelegate(cp);
		}

	}

	public final Map<String, Map<Integer, Module>> getDeployedModules() {
		Assert.notNull(moduleDeployer, "ModuleDeployer is required to get deployed modules.");
		return moduleDeployer.getDeployedModules();
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
		return this.messageBus;
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

	public final void deleteStream(String name) {
		streamDeployer.delete(name);
	}

	public final Module getModule(String streamName, String moduleName, int index) {
		Map<Integer, Module> modules = getDeployedModules().get(streamName);
		return (modules != null) ? modules.get(index) : null;
	}

	public ZooKeeperConnection zooKeeperConnection() {
		return this.zooKeeperConnection;
	}

	/**
	 * Add a {@link PathChildrenCacheListener} for the given path.
	 * 
	 * @param path the path whose children to listen to
	 * @param listener the children listener
	 */
	public void addPathListener(String path, PathChildrenCacheListener listener) {
		PathChildrenCache cache = mapChildren.get(path);
		if (cache == null) {
			mapChildren.put(path, cache = new PathChildrenCache(zooKeeperConnection.getClient(), path, true));
			try {
				cache.start();
			}
			catch (Exception e) {
				throw e instanceof RuntimeException ? ((RuntimeException) e) : new RuntimeException(e);
			}
		}
		cache.getListenable().addListener(listener);
	}

	/**
	 * Remove a {@link PathChildrenCacheListener} for the given path.
	 * 
	 * @param path the path whose children to listen to
	 * @param listener the children listener
	 */
	public void removePathListener(String path, PathChildrenCacheListener listener) {
		PathChildrenCache cache = mapChildren.get(path);
		if (cache != null) {
			cache.getListenable().removeListener(listener);
			if (cache.getListenable().size() == 0) {
				try {
					cache.close();
					mapChildren.remove(path);
				}
				catch (Exception e) {
					throw e instanceof RuntimeException ? ((RuntimeException) e) : new RuntimeException(e);
				}
			}
		}
	}

	private final boolean waitForStreamOp(StreamDefinition definition, boolean isDeploy) {
		final int MAX_TRIES = 40;
		int tries = 1;
		boolean done = false;

		while (!done && tries <= MAX_TRIES) {
			done = true;
			int i = definition.getModuleDefinitions().size();
			for (ModuleDefinition module : definition.getModuleDefinitions()) {
				Module deployedModule = getModule(definition.getName(), module.getName(), --i);
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
