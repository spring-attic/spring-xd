/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.xd.dirt.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;

import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.DeploymentUnitStatus.State;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.integration.bus.MessageBusSupport;
import org.springframework.xd.dirt.module.DelegatingModuleRegistry;
import org.springframework.xd.dirt.module.ModuleDeployer;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.server.singlenode.SingleNodeApplication;
import org.springframework.xd.dirt.stream.JobDefinitionRepository;
import org.springframework.xd.dirt.stream.JobRepository;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamDeployer;
import org.springframework.xd.dirt.stream.StreamRepository;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;
import org.springframework.xd.module.core.Module;

/**
 * A helper class that provides methods used for testing streams with {@link SingleNodeApplication}. It exposes
 * components and methods used for stream creation, deployment, and destruction and provides access to the
 * {@link MessageBus}. Additionally, it supports registration of modules contained in a local resource location
 * (default: "file:./config").
 *
 * Note that all operations block until the expected state is verified or the operation times out.
 *
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 */
public class SingleNodeIntegrationTestSupport {

	private static final Map<String, String> EMPTY_PROPERTIES = Collections.emptyMap();

	private final JobDefinitionRepository jobDefinitionRepository;

	private final StreamDefinitionRepository streamDefinitionRepository;

	private final StreamRepository streamRepository;

	private final JobRepository jobRepository;

	private final StreamDeployer streamDeployer;

	private final MessageBusSupport messageBus;

	private final ModuleDeployer moduleDeployer;

	private final ZooKeeperConnection zooKeeperConnection;

	private final ResourceStateVerifier streamResourceStateVerifier;

	private final ResourceStateVerifier jobResourceStateVerifier;

	private final SingleNodeApplication application;

	private final Map<String, PathChildrenCache> mapChildren = new HashMap<String, PathChildrenCache>();


	/**
	 * Constructor useful for testing custom modules
	 *
	 * @param application the {@link SingleNodeApplication}
	 */
	public SingleNodeIntegrationTestSupport(SingleNodeApplication application) {
		Assert.notNull(application, "SingleNodeApplication must not be null");
		this.application = application;
		streamDefinitionRepository = application.pluginContext().getBean(StreamDefinitionRepository.class);
		jobDefinitionRepository = application.pluginContext().getBean(JobDefinitionRepository.class);
		streamRepository = application.pluginContext().getBean(StreamRepository.class);
		jobRepository = application.pluginContext().getBean(JobRepository.class);
		streamResourceStateVerifier = new ResourceStateVerifier(streamRepository, streamDefinitionRepository);
		jobResourceStateVerifier = new ResourceStateVerifier(jobRepository, jobDefinitionRepository);
		streamDeployer = application.adminContext().getBean(StreamDeployer.class);
		messageBus = application.pluginContext().getBean(MessageBusSupport.class);
		zooKeeperConnection = application.adminContext().getBean(ZooKeeperConnection.class);
		moduleDeployer = application.containerContext().getBean(ModuleDeployer.class);
	}

	public final void addModuleRegistry(ModuleRegistry moduleRegistry) {
		DelegatingModuleRegistry cmr1 = application.pluginContext().getBean(DelegatingModuleRegistry.class);
		cmr1.addDelegate(moduleRegistry);
		DelegatingModuleRegistry cmr2 = application.adminContext().getBean(DelegatingModuleRegistry.class);
		if (cmr1 != cmr2) {
			cmr2.addDelegate(moduleRegistry);
		}
	}

	/**
	 * Get all currently deployed modules.
	 * @return a map keyed by stream name. Each entry is a map of the modules by module index(stream left to right order).
	 */
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

	public final ResourceStateVerifier streamStateVerifier() {
		return streamResourceStateVerifier;
	}

	public final ResourceStateVerifier jobStateVerifier() {
		return jobResourceStateVerifier;
	}

	public final JobRepository jobRepository() {
		return jobRepository;
	}

	public final JobDefinitionRepository jobDefinitionRepository() {
		return jobDefinitionRepository;
	}

	public final StreamDefinitionRepository streamDefinitionRepository() {
		return streamDefinitionRepository;
	}

	public final MessageBusSupport messageBus() {
		return this.messageBus;
	}

	/**
	 * Deploy a stream.
	 * @param definition the stream definition
	 * @return true if the operation succeeded
	 */
	public final boolean deployStream(StreamDefinition definition) {
		return waitForDeploy(definition);
	}

	/**
	 * Deploy a stream with properties.
	 * @param definition  the stream definition
	 * @param properties the deployment properties
	 * @return true if the operation succeeded
	 */
	public final boolean deployStream(StreamDefinition definition, Map<String, String> properties) {
		return waitForDeploy(definition, properties);
	}

	/**
	 * Deploy a stream with properties and allow return on incomplete deployment
	 * @param definition the stream definition
	 * @param properties the deployment properties
	 * @param allowIncomplete allow incomplete as well as deployed state
	 * @return true if operation succeeded
	 */
	public final boolean deployStream(StreamDefinition definition, Map<String, String> properties,
			boolean allowIncomplete) {
		return waitForDeploy(definition, properties, allowIncomplete);
	}

	/**
	 * Create and deploy a stream.
	 * @param definition the stream definition
	 * @return true if the operation succeeded.
	 */
	public final boolean createAndDeployStream(StreamDefinition definition) {
		streamDeployer.save(definition);
		return waitForDeploy(definition);
	}

	/**
	 * Undeploy a stream.
	 * @param definition the stream definition
	 * @return true if the operation succeeded
	 */
	public final boolean undeployStream(StreamDefinition definition) {
		return waitForUndeploy(definition);
	}

	/**
	 * Undeploy and destroy a stream.
	 * @param definition the stream definition
	 * @return true if the operation succeeded
	 */
	public final boolean undeployAndDestroyStream(StreamDefinition definition) {
		boolean result = waitForUndeploy(definition);
		streamDeployer.delete(definition.getName());
		return result;
	}

	/**
	 * Delete a stream.
	 * @param name  the stream name
	 */
	public final void deleteStream(String name) {
		streamDeployer.delete(name);
	}

	/**
	 * Get a deployed module instance.
	 * @param streamName the stream name
	 * @param moduleName the module name
	 * @param index the index of the module in the stream, ordered left to right starting with 0.
	 * @return the module
	 */
	public final Module getModule(String streamName, String moduleName, int index) {
		Map<Integer, Module> modules = getDeployedModules().get(streamName);
		return (modules != null) ? modules.get(index) : null;
	}

	/**
	 *
	 * @return  the zookeeperConnection
	 */
	public ZooKeeperConnection zooKeeperConnection() {
		return this.zooKeeperConnection;
	}

	/**
	 * Add a {@link PathChildrenCacheListener} for the given path.
	 *
	 * @param path the path whose children to listen to
	 * @param listener the children listener
	 */
	public void addPathListener(final String path, PathChildrenCacheListener listener) {
		PathChildrenCache cache = mapChildren.get(path);
		if (cache == null) {
			mapChildren.put(path, cache = new PathChildrenCache(zooKeeperConnection.getClient(), path, true));
			try {
				cache.getListenable().addListener(listener);
				cache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
			}
			catch (Exception e) {
				throw ZooKeeperUtils.wrapThrowable(e);
			}
		}
		else {
			cache.getListenable().addListener(listener);
		}
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
					throw ZooKeeperUtils.wrapThrowable(e);
				}
			}
		}
	}

	private boolean waitForUndeploy(StreamDefinition definition) {
		streamDeployer.undeploy(definition.getName());
		State state = streamResourceStateVerifier.waitForUndeploy(definition.getName());
		return state.equals(State.undeployed);
	}

	private boolean waitForDeploy(StreamDefinition definition) {
		return waitForDeploy(definition, EMPTY_PROPERTIES);
	}

	private boolean waitForDeploy(StreamDefinition definition, Map<String, String> properties) {
		streamDeployer.deploy(definition.getName(), properties);
		State state = streamStateVerifier().waitForDeploy(definition.getName());
		if (state.equals(State.deployed)) {
			return true;
		}
		return false;

	}

	private boolean waitForDeploy(StreamDefinition definition, Map<String, String> properties, boolean allowIncomplete) {
		streamDeployer.deploy(definition.getName(), properties);
		State state = streamResourceStateVerifier.waitForDeploy(definition.getName(), allowIncomplete);
		if (allowIncomplete) {
		  return state.equals(State.deployed) || state.equals(State.incomplete);
		}
		return state.equals(State.deployed);
	}
}
