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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.OrderComparator;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.ModuleFactory;
import org.springframework.xd.module.core.Plugin;

/**
 * Handles deployment related tasks instantiates {@link Module}s accordingly, applying {@link Plugin} logic
 * to them.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Ilayaperumal Gopinathan
 * @author David Turanski
 */
public class ModuleDeployer implements ApplicationContextAware, InitializingBean {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile ApplicationContext context;

	private volatile ApplicationContext globalContext;

	private final ConcurrentMap<String, Map<Integer, Module>> deployedModules = new ConcurrentHashMap<String, Map<Integer, Module>>();

	private volatile List<Plugin> plugins;

	private final ModuleFactory moduleFactory;

	public Map<String, Map<Integer, Module>> getDeployedModules() {
		return deployedModules;
	}

	public ModuleDeployer(ModuleFactory moduleFactory) {
		this.moduleFactory = moduleFactory;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) {
		this.context = context;
		this.globalContext = context.getParent().getParent();
	}

	@Override
	public void afterPropertiesSet() {
		this.plugins = new ArrayList<Plugin>(this.context.getParent().getBeansOfType(Plugin.class).values());
		OrderComparator.sort(this.plugins);
	}


	// todo: when refactoring to ZK-based deployment, keep this method but remove the private one
	// but notice the use of 'group' which is abstract so it can also support jobs (not just streams)
	// that terminology needs to change since group will be used in criteria expressions. Most likely we
	// need to be more explicit about jobs vs. streams rather than trying to genericize into one concept.
	public void deployAndStore(Module module, ModuleDescriptor descriptor) {
		this.deployAndStore(module, descriptor.getGroup(), descriptor.getIndex());
	}

	// todo: same general idea as deployAndStore above
	public void undeploy(ModuleDescriptor moduleDescriptor) {
		this.handleUndeploy(moduleDescriptor.getGroup(), moduleDescriptor.getIndex());
	}

	/**
	 * Create a module based on the provided {@link ModuleDescriptor}, and
	 * {@link ModuleDeploymentProperties}.
	 *
	 * @param moduleDescriptor descriptor for the module
	 * @param deploymentProperties deployment related properties for the module
	 *
	 * @return new module instance
	 */
	public Module createModule(ModuleDescriptor moduleDescriptor,
			ModuleDeploymentProperties deploymentProperties) {
		return moduleFactory.newInstance(moduleDescriptor,deploymentProperties);
	}

	private void deploy(Module module) {
		this.preProcessModule(module);
		module.initialize();
		this.postProcessModule(module);
		module.start();
	}

	private void handleUndeploy(String group, int index) {
		Map<Integer, Module> modules = this.deployedModules.get(group);
		if (modules != null) {
			Module module = modules.remove(index);
			if (modules.size() == 0) {
				this.deployedModules.remove(group);
			}
			if (module != null) {
				this.destroyModule(module);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Ignoring undeploy - module with index " + index + " from group " + group
							+ " is not deployed here");
				}
			}
		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace("Ignoring undeploy - group not deployed here: " + group);
			}
		}
	}

	private void destroyModule(Module module) {
		if (logger.isInfoEnabled()) {
			logger.info("removed " + module.toString());
		}
		this.beforeShutdown(module);
		module.stop();
		this.removeModule(module);
		module.destroy();
	}

	/**
	 * Get the list of supported plugins for the given module.
	 *
	 * @param module
	 * @return list supported list of plugins
	 */
	private List<Plugin> getSupportedPlugins(Module module) {
		List<Plugin> supportedPlugins = new ArrayList<Plugin>();
		if (this.plugins != null) {
			for (Plugin plugin : this.plugins) {
				if (plugin.supports(module)) {
					supportedPlugins.add(plugin);
				}
			}
		}
		return supportedPlugins;
	}

	private void deployAndStore(Module module, String group, int index) {
		module.setParentContext(this.globalContext);
		this.deploy(module);
		if (logger.isInfoEnabled()) {
			logger.info("deployed " + module.toString());
		}
		this.deployedModules.putIfAbsent(group, new HashMap<Integer, Module>());
		this.deployedModules.get(group).put(index, module);
	}

	/**
	 * Allow plugins to contribute properties (e.g. "stream.name") calling module.addProperties(properties), etc.
	 */
	private void preProcessModule(Module module) {
		for (Plugin plugin : this.getSupportedPlugins(module)) {
			plugin.preProcessModule(module);
		}
	}

	/**
	 * Allow plugins to perform other configuration after the module is initialized but before it is started.
	 */
	private void postProcessModule(Module module) {
		for (Plugin plugin : this.getSupportedPlugins(module)) {
			plugin.postProcessModule(module);
		}
	}

	private void removeModule(Module module) {
		for (Plugin plugin : this.getSupportedPlugins(module)) {
			plugin.removeModule(module);
		}
	}

	private void beforeShutdown(Module module) {
		for (Plugin plugin : this.getSupportedPlugins(module)) {
			try {
				plugin.beforeShutdown(module);
			}
			catch (IllegalStateException e) {
				if (logger.isWarnEnabled()) {
					logger.warn("Failed to invoke plugin " + plugin.getClass().getSimpleName()
							+ " during shutdown. " + e.getMessage());
					if (logger.isDebugEnabled()) {
						logger.debug(e);
					}
				}
			}
		}
	}

}
