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
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.OrderComparator;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.ModuleDescriptor;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.CompositeModule;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.Plugin;
import org.springframework.xd.module.core.SimpleModule;
import org.springframework.xd.module.options.ModuleOptions;
import org.springframework.xd.module.support.ParentLastURLClassLoader;

/**
 * Listens for deployment request messages and instantiates {@link Module}s accordingly, applying {@link Plugin} logic
 * to them.
 * 
 * @author Mark Fisher
 * @author Gary Russell
 * @author Ilayaperumal Gopinathan
 */
public class ModuleDeployer implements ApplicationContextAware, BeanClassLoaderAware, InitializingBean, DisposableBean {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile ApplicationContext context;

	private volatile ApplicationContext globalContext;

	private final ConcurrentMap<String, Map<Integer, Module>> deployedModules = new ConcurrentHashMap<String, Map<Integer, Module>>();

	private volatile List<Plugin> plugins;

	private final ModuleDefinitionRepository moduleDefinitionRepository;

	private ClassLoader parentClassLoader;

	public ModuleDeployer(ModuleDefinitionRepository moduleDefinitionRepository) {
		Assert.notNull(moduleDefinitionRepository, "moduleDefinitionRepository must not be null");
		this.moduleDefinitionRepository = moduleDefinitionRepository;
	}

	public Map<String, Map<Integer, Module>> getDeployedModules() {
		return deployedModules;
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

	@Override
	public void destroy() throws Exception {
		for (Entry<String, Map<Integer, Module>> entry : this.deployedModules.entrySet()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Destroying group:" + entry.getKey());
			}
		}
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.parentClassLoader = classLoader;
	}

	private Module createModule(ModuleDeploymentRequest request, ModuleOptions moduleOptions) {
		if (request instanceof CompositeModuleDeploymentRequest) {
			return createCompositeModule((CompositeModuleDeploymentRequest) request, moduleOptions);
		}
		else {
			return createSimpleModule(request, moduleOptions);
		}
	}

	private Module createCompositeModule(CompositeModuleDeploymentRequest compositeRequest,
			ModuleOptions moduleOptionsForComposite) {
		List<ModuleDeploymentRequest> children = compositeRequest.getChildren();
		Assert.notEmpty(children, "child module list must not be empty");

		List<Module> childrenModules = new ArrayList<Module>(children.size());
		for (ModuleDeploymentRequest childRequest : children) {
			ModuleOptions narrowedModuleOptions = new PrefixNarrowingModuleOptions(moduleOptionsForComposite,
					childRequest.getModule());
			childrenModules.add(createModule(childRequest, narrowedModuleOptions));
		}

		String group = compositeRequest.getGroup();
		int index = compositeRequest.getIndex();
		String sourceChannelName = compositeRequest.getSourceChannelName();
		String sinkChannelName = compositeRequest.getSinkChannelName();
		DeploymentMetadata deploymentMetadata = new DeploymentMetadata(group, index, sourceChannelName, sinkChannelName);

		String moduleName = compositeRequest.getModule();
		ModuleType moduleType = compositeRequest.getType();

		return new CompositeModule(moduleName, moduleType, childrenModules, deploymentMetadata);
	}

	private Module createSimpleModule(ModuleDeploymentRequest request, ModuleOptions moduleOptions) {
		String group = request.getGroup();
		int index = request.getIndex();
		String name = request.getModule();
		ModuleType type = request.getType();
		ModuleDefinition definition = this.moduleDefinitionRepository.findByNameAndType(name, type);
		Assert.notNull(definition, "No moduleDefinition for " + name + ":" + type);
		DeploymentMetadata metadata = new DeploymentMetadata(group, index, request.getSourceChannelName(),
				request.getSinkChannelName());

		@SuppressWarnings("resource")
		ClassLoader classLoader = (definition.getClasspath() == null) ? null
				: new ParentLastURLClassLoader(definition.getClasspath(), parentClassLoader);

		Module module = new SimpleModule(definition, metadata, classLoader, moduleOptions);
		return module;
	}

	// todo: when refactoring to ZK-based deployment, keep this method but remove the private one
	// but notice the use of 'group' which is abstract so it can also support jobs (not just streams)
	// that terminology needs to change since group is part of a deployment manifest. Most likely we
	// need to be more explicit about jobs vs. streams rather than trying to genericize into one concept.
	public void deployAndStore(Module module, ModuleDescriptor descriptor) {
		this.deployAndStore(module, descriptor.getStreamName(), descriptor.getIndex());
	}

	// todo: same general idea as deployAndStore above
	public void undeploy(ModuleDescriptor moduleDescriptor) {
		this.handleUndeploy(moduleDescriptor.getStreamName(), moduleDescriptor.getIndex());
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
			plugin.beforeShutdown(module);
		}
	}

}
