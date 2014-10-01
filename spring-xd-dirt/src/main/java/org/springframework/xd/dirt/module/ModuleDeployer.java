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

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.OrderComparator;
import org.springframework.util.Assert;
import org.springframework.validation.BindException;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.core.CompositeModule;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.Plugin;
import org.springframework.xd.module.core.SimpleModule;
import org.springframework.xd.module.options.ModuleOptions;
import org.springframework.xd.module.options.ModuleOptionsMetadata;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;
import org.springframework.xd.module.options.PrefixNarrowingModuleOptions;
import org.springframework.xd.module.support.ParentLastURLClassLoader;

/**
 * Handles deployment related tasks instantiates {@link Module}s accordingly, applying {@link Plugin} logic
 * to them.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Ilayaperumal Gopinathan
 * @author David Turanski
 */
public class ModuleDeployer implements ApplicationContextAware, InitializingBean, BeanClassLoaderAware {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile ApplicationContext context;

	private volatile ApplicationContext globalContext;

	private final ConcurrentMap<String, Map<Integer, Module>> deployedModules = new ConcurrentHashMap<String, Map<Integer, Module>>();

	private volatile ClassLoader parentClassLoader;

	private final ModuleOptionsMetadataResolver moduleOptionsMetadataResolver;

	private volatile List<Plugin> plugins;

	/**
	 *
	 * @param moduleOptionsMetadataResolver the moduleOptionsMetadataResolver.
	 */
	public ModuleDeployer(ModuleOptionsMetadataResolver moduleOptionsMetadataResolver) {
		this.moduleOptionsMetadataResolver = moduleOptionsMetadataResolver;
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.parentClassLoader = classLoader;
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
		ModuleOptions moduleOptions = this.safeModuleOptionsInterpolate(moduleDescriptor);
		return (moduleDescriptor.isComposed())
				? createComposedModule(moduleDescriptor, moduleOptions, deploymentProperties)
				: createSimpleModule(moduleDescriptor, moduleOptions, deploymentProperties);
	}

	/**
	 * Create a simple module based on the provided {@link ModuleDescriptor}, {@link org.springframework.xd.module.options.ModuleOptions}, and
	 * {@link org.springframework.xd.module.ModuleDeploymentProperties}.
	 *
	 * @param moduleDescriptor descriptor for the composed module
	 * @param moduleOptions module options for the composed module
	 * @param deploymentProperties deployment related properties for the composed module
	 *
	 * @return new simple module instance
	 *
	 */
	private Module createSimpleModule(ModuleDescriptor moduleDescriptor, ModuleOptions moduleOptions,
			ModuleDeploymentProperties deploymentProperties) {
		ModuleDefinition definition = moduleDescriptor.getModuleDefinition();
		ClassLoader classLoader = (definition.getClasspath() == null) ? null
				: new ParentLastURLClassLoader(definition.getClasspath(), parentClassLoader);
		return new SimpleModule(moduleDescriptor, deploymentProperties, classLoader, moduleOptions);
	}

	/**
	 * Create a composed module based on the provided {@link ModuleDescriptor}, {@link org.springframework.xd.module.options.ModuleOptions}, and
	 * {@link org.springframework.xd.module.ModuleDeploymentProperties}.
	 *
	 * @param compositeDescriptor descriptor for the composed module
	 * @param options module options for the composed module
	 * @param deploymentProperties deployment related properties for the composed module
	 *
	 * @return new composed module instance
	 *
	 * @see ModuleDescriptor#isComposed
	 */
	private Module createComposedModule(ModuleDescriptor compositeDescriptor,
			ModuleOptions options, ModuleDeploymentProperties deploymentProperties) {

		List<ModuleDescriptor> children = compositeDescriptor.getChildren();
		Assert.notEmpty(children, "child module list must not be empty");

		List<Module> childrenModules = new ArrayList<Module>(children.size());
		for (ModuleDescriptor childRequest : children) {
			ModuleOptions narrowedOptions = new PrefixNarrowingModuleOptions(options, childRequest.getModuleName());
			// due to parser results being reversed, we add each at index 0
			// todo: is it right to pass the composite deploymentProperties here?
			childrenModules.add(0, createSimpleModule(childRequest, narrowedOptions, deploymentProperties));
		}
		return new CompositeModule(compositeDescriptor, deploymentProperties, childrenModules);
	}

	/**
	 * Takes a request and returns an instance of {@link ModuleOptions} bound with the request parameters. Binding is
	 * assumed to not fail, as it has already been validated on the admin side.
	 *
	 * @param descriptor module descriptor for which to bind request parameters
	 *
	 * @return module options bound with request parameters
	 */
	private ModuleOptions safeModuleOptionsInterpolate(ModuleDescriptor descriptor) {
		Map<String, String> parameters = descriptor.getParameters();
		ModuleOptionsMetadata moduleOptionsMetadata = moduleOptionsMetadataResolver.resolve(descriptor.getModuleDefinition());
		try {
			return moduleOptionsMetadata.interpolate(parameters);
		}
		catch (BindException e) {
			// Can't happen as parser should have already validated options
			throw new IllegalStateException(e);
		}
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
