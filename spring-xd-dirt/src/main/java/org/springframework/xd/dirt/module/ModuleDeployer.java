/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.module;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.concurrent.GuardedBy;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.OrderComparator;
import org.springframework.util.Assert;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.ModuleFactory;
import org.springframework.xd.module.core.Plugin;

/**
 * Handles the creation, deployment, and un-deployment of {@link Module modules}.
 * Appropriate {@link Plugin} logic is applied throughout the deployment/
 * un-deployment lifecycle.
 * <p>
 * In order to initialize modules with the correct application context,
 * this class maintains a reference to the global application context.
 * See <a href="http://docs.spring.io/autorepo/docs/spring-xd/current/reference/html/#XD-Spring-Application-Contexts">
 * the reference documentation</a> for more details.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Ilayaperumal Gopinathan
 * @author David Turanski
 * @author Patrick Peralta
 */
public class ModuleDeployer implements ApplicationContextAware, InitializingBean {

	/**
	 * Logger.
	 */
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * The container application context.
	 */
	private volatile ApplicationContext context;

	/**
	 * The global top level application context. This is used as the parent
	 * application context for loaded modules.
	 */
	private volatile ApplicationContext globalContext;

	/**
	 * Map of deployed modules. Key is the group/deployment unit name,
	 * value is a map of module index to module.
	 */
	@GuardedBy("this")
	private final Map<String, Map<Integer, Module>> deployedModules = new HashMap<String, Map<Integer, Module>>();

	/**
	 * List of registered plugins.
	 */
	@GuardedBy("this")
	private final List<Plugin> plugins = new ArrayList<Plugin>();

	/**
	 * Module factory for creating new {@link Module} instances.
	 */
	private final ModuleFactory moduleFactory;

	/**
	 * Construct a ModuleDeployer.
	 *
	 * @param moduleFactory module factory for creating new {@link Module} instances
	 */
	public ModuleDeployer(ModuleFactory moduleFactory) {
		this.moduleFactory = moduleFactory;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @param context the container application context
	 */
	@Override
	public void setApplicationContext(ApplicationContext context) {
		this.context = context;

		ApplicationContext global = null;
		try {
			// TODO: evaluate
			// The application context hierarchy is arranged as such:
			//
			//         Global Context
			//              ^
			//     Shared Server Context
			//              ^
			//        Plugin Context
			//              ^
			//       Container Context
			//
			// The global context is supposed to be the parent
			// context for deployed modules which means the
			// context should be obtained via
			// context.getParent().getParent().getParent().
			// However this is causing shell test failures;
			// in particular NoSuchBeanDefinitionExceptions
			// for aggregateCounterRepository, fieldValueCounterRepository,
			// counterRepository, etc. This should be further evaluated.
//			global = context.getParent().getParent().getParent();
			global = context.getParent().getParent();
		}
		catch (NullPointerException e) {
			logger.trace("Exception looking up global application context", e);
			// npe handled by assert below
		}

		Assert.notNull(global, "Global application context not found");
		this.globalContext = global;
	}

	@Override
	public synchronized void afterPropertiesSet() {
		if (!plugins.isEmpty()) {
			plugins.clear();
		}
		plugins.addAll(this.context.getParent().getBeansOfType(Plugin.class).values());
		OrderComparator.sort(this.plugins);
	}

	/**
	 * Return a read-only map of deployed modules. Key is the group/deployment
	 * unit name, value is a map of module index to module.
	 *
	 * @return map of deployed modules
	 */
	public synchronized Map<String, Map<Integer, Module>> getDeployedModules() {
		Map<String, Map<Integer, Module>> map = new HashMap<String, Map<Integer, Module>>();
		for (Map.Entry<String, Map<Integer, Module>> entry : this.deployedModules.entrySet()) {
			map.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
		}
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Create a module based on the provided {@link ModuleDescriptor} and
	 * {@link ModuleDeploymentProperties}.
	 *
	 * @param moduleDescriptor descriptor for the module
	 * @param deploymentProperties deployment related properties for the module
	 *
	 * @return new module instance
	 */
	public Module createModule(ModuleDescriptor moduleDescriptor,
			ModuleDeploymentProperties deploymentProperties) {
		return moduleFactory.createModule(moduleDescriptor, deploymentProperties);
	}

	/**
	 * Deploy the given module to this container. This action includes
	 * <ul>
	 *     <li>applying the appropriate plugins</li>
	 *     <li>setting the parent application context</li>
	 *     <li>starting the module</li>
	 *     <li>registering the module with this container</li>
	 * </ul>
	 *
	 * @param module the module to deploy
	 * @param descriptor descriptor for the module instance
	 */
	public synchronized void deploy(Module module, ModuleDescriptor descriptor) {
		String group = descriptor.getGroup();

		module.setParentContext(this.globalContext);
		doDeploy(module);
		logger.info("Deployed {}", module);
		Map<Integer, Module> modules = this.deployedModules.get(group);
		if (modules == null) {
			modules = new HashMap<Integer, Module>();
			this.deployedModules.put(group, modules);
		}
		modules.put(descriptor.getIndex(), module);
	}

	/**
	 * Apply plugins to the module and start it.
	 *
	 * @param module module to deploy
	 */
	private void doDeploy(Module module) {
		preProcessModule(module);
		module.initialize();
		postProcessModule(module);
		module.start();
	}

	/**
	 * Allow plugins to contribute properties (e.g. "stream.name")
	 * calling {@link Module#addProperties(java.util.Properties)}, etc.
	 */
	private void preProcessModule(Module module) {
		for (Plugin plugin : getSupportedPlugins(module)) {
			plugin.preProcessModule(module);
		}
	}

	/**
	 * Allow plugins to perform other configuration after the module
	 * is initialized but before it is started.
	 */
	private void postProcessModule(Module module) {
		for (Plugin plugin : getSupportedPlugins(module)) {
			plugin.postProcessModule(module);
		}
	}

	/**
	 * Shut down the module indicated by {@code moduleDescriptor}
	 * and remove its registration from the container. Lifecycle
	 * plugins are applied during undeployment.
	 *
	 * @param moduleDescriptor descriptor for module to be undeployed
	 */
	public synchronized void undeploy(ModuleDescriptor moduleDescriptor) {
		Introspector.flushCaches(); // This is to prevent classloader leakage
		String group = moduleDescriptor.getGroup();
		int index = moduleDescriptor.getIndex();
		Map<Integer, Module> modules = deployedModules.get(group);
		if (modules != null) {
			Module module = modules.remove(index);
			if (modules.isEmpty()) {
				deployedModules.remove(group);
			}
			if (module != null) {
				destroyModule(module);
			}
			else {
				logger.debug("Ignoring undeploy - module with index {} from group {} is not deployed", index, group);
			}
		}
		else {
			logger.trace("Ignoring undeploy - group not deployed here: {}", group);
		}
	}

	/**
	 * Apply lifecycle plugins and shut down the module.
	 *
	 * @param module module to shut down and destroy
	 */
	private void destroyModule(Module module) {
		logger.info("Removed {}", module);
		beforeShutdown(module);
		module.stop();
		removeModule(module);
		module.destroy();
	}

	/**
	 * Apply shutdown lifecycle plugins for the given module.
	 *
	 * @param module module to shutdown
	 */
	private void beforeShutdown(Module module) {
		for (Plugin plugin : getSupportedPlugins(module)) {
			try {
				plugin.beforeShutdown(module);
			}
			catch (IllegalStateException e) {
				logger.warn("Failed to invoke plugin {} during shutdown: {}",
						plugin.getClass().getSimpleName(), e.getMessage());
				logger.debug("Full stack trace", e);
			}
		}
	}

	/**
	 * Apply remove lifecycle plugins for the given module.
	 *
	 * @param module module that has been shutdown
	 */
	private void removeModule(Module module) {
		for (Plugin plugin : getSupportedPlugins(module)) {
			plugin.removeModule(module);
		}
	}

	/**
	 * Return an {@link Iterable} over the list of supported plugins for the given module.
	 *
	 * @param module the module for which to obtain supported plugins
	 * @return iterable of supported plugins for a module
	 */
	private Iterable<Plugin> getSupportedPlugins(Module module) {
		return Iterables.filter(this.plugins, new ModulePluginPredicate(module));
	}


	/**
	 * Predicate used to determine if a plugin supports a module.
	 */
	private class ModulePluginPredicate implements Predicate<Plugin> {
		private final Module module;

		private ModulePluginPredicate(Module module) {
			this.module = module;
		}

		@Override
		public boolean apply(Plugin plugin) {
			return plugin.supports(this.module);
		}
	}

}
