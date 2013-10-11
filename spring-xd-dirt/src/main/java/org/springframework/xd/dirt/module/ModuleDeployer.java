/*
 * Copyright 2013 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.dirt.event.ModuleDeployedEvent;
import org.springframework.xd.dirt.event.ModuleUndeployedEvent;
import org.springframework.xd.dirt.plugins.job.JobPlugin;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.Module;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.Plugin;
import org.springframework.xd.module.SimpleModule;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Listens for deployment request messages and instantiates {@link Module}s accordingly, applying {@link Plugin} logic
 * to them.
 * 
 * @author Mark Fisher
 * @author Gary Russell
 * @author Ilayaperumal Gopinathan
 */
public class ModuleDeployer extends AbstractMessageHandler implements ApplicationContextAware,
		ApplicationEventPublisherAware, BeanClassLoaderAware {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile ApplicationContext deployerContext;

	private volatile ApplicationContext commonContext;

	private volatile ApplicationEventPublisher eventPublisher;

	private final ObjectMapper mapper = new ObjectMapper();

	private final ConcurrentMap<String, Map<Integer, Module>> deployedModules = new ConcurrentHashMap<String, Map<Integer, Module>>();

	private volatile Map<String, Plugin> plugins;

	private final ModuleRegistry moduleRegistry;

	private ClassLoader parentClassLoader;

	public ModuleDeployer(ModuleRegistry moduleRegistry) {
		Assert.notNull(moduleRegistry, "moduleRegistry must not be null");
		this.moduleRegistry = moduleRegistry;
	}

	public Map<String, Map<Integer, Module>> getDeployedModules() {
		return deployedModules;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) {
		this.deployerContext = context;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void onInit() {
		this.plugins = this.deployerContext.getBeansOfType(Plugin.class);
		ClassPathXmlApplicationContext commonContext = new ClassPathXmlApplicationContext(
				new String[] { XDContainer.XD_INTERNAL_CONFIG_ROOT + "module-common.xml" }, false);
		ApplicationContext globalContext = deployerContext.getParent();
		commonContext.setParent(globalContext);

		for (Plugin plugin : plugins.values()) {
			plugin.preProcessSharedContext(commonContext);
		}
		commonContext.refresh();
		this.commonContext = commonContext;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.parentClassLoader = classLoader;
	}

	@Override
	protected synchronized void handleMessageInternal(Message<?> message) throws Exception {
		ModuleDeploymentRequest request = this.mapper.readValue(message.getPayload().toString(),
				ModuleDeploymentRequest.class);
		if (request.isRemove()) {
			handleUndeploy(request);
		}
		else if (request.isLaunch()) {
			handleLaunch(request, message);
		}
		else {
			handleDeploy(request, message);
		}
	}

	private void handleDeploy(ModuleDeploymentRequest request, Message<?> message) {
		String group = request.getGroup();
		int index = request.getIndex();
		String name = request.getModule();
		ModuleType type = request.getType();
		ModuleDefinition definition = this.moduleRegistry.findDefinition(name, type);
		Assert.notNull(definition, "No moduleDefinition for " + name + ":" + type);
		DeploymentMetadata metadata = new DeploymentMetadata(group, index, request.getSourceChannelName(),
				request.getSinkChannelName());

		ClassLoader classLoader = (definition.getClasspath() == null) ? null
				: new ParentLastURLClassLoader(definition.getClasspath(), parentClassLoader);

		Module module = new SimpleModule(definition, metadata, classLoader);
		module.setParentContext(this.commonContext);
		Object properties = message.getHeaders().get("properties");
		if (properties instanceof Properties) {
			module.addProperties((Properties) properties);
		}
		Map<String, String> parameters = request.getParameters();
		if (!CollectionUtils.isEmpty(parameters)) {
			Properties parametersAsProps = new Properties();
			parametersAsProps.putAll(parameters);
			module.addProperties(parametersAsProps);
		}
		this.deployModule(module);
		if (logger.isInfoEnabled()) {
			logger.info("deployed " + module.toString());
		}
		this.deployedModules.putIfAbsent(group, new HashMap<Integer, Module>());
		this.deployedModules.get(group).put(index, module);
	}

	private void deployModule(Module module) {
		this.preProcessModule(module);
		module.initialize();
		this.postProcessModule(module);
		module.start();
		this.fireModuleDeployedEvent(module);
	}

	private void handleUndeploy(ModuleDeploymentRequest request) {
		String group = request.getGroup();
		Map<Integer, Module> modules = this.deployedModules.get(group);
		if (modules != null) {
			int index = request.getIndex();
			Module module = modules.remove(index);
			if (modules.size() == 0) {
				this.deployedModules.remove(group);
			}
			if (module != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("removed " + module.toString());
				}
				// TODO: add beforeShutdown and/or afterShutdown callbacks?
				this.beforeShutdown(module);
				module.stop();
				this.removeModule(module);
				module.destroy();
				this.fireModuleUndeployedEvent(module);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Ignoring undeploy - module not deployed here: " + request);
				}
			}
		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace("Ignoring undeploy - group not deployed here: " + request);
			}
		}
	}

	private void handleLaunch(ModuleDeploymentRequest request, Message<?> message) {
		String group = request.getGroup();
		Map<Integer, Module> modules = this.deployedModules.get(group);
		if (modules != null) {
			processLaunchRequest(modules, request);
		}
		else {
			// Deploy the job module and then launch
			handleDeploy(request, message);
			processLaunchRequest(this.deployedModules.get(group), request);
		}
	}

	private void processLaunchRequest(Map<Integer, Module> modules, ModuleDeploymentRequest request) {
		Module module = modules.get(request.getIndex());
		// Since the request parameter may change on each launch request,
		// the request parameters are not added to module properties
		if (logger.isDebugEnabled()) {
			logger.debug("launching " + module.toString());
		}
		launchModule(module, request.getParameters());
	}

	/**
	 * Allow plugins to contribute properties (e.g. "stream.name") calling module.addProperties(properties), etc.
	 */
	private void preProcessModule(Module module) {
		if (this.plugins != null) {
			for (Plugin plugin : this.plugins.values()) {
				plugin.preProcessModule(module);
			}
		}
	}

	/**
	 * Allow plugins to perform other configuration after the module is initialized but before it is started.
	 */
	private void postProcessModule(Module module) {
		if (this.plugins != null) {
			for (Plugin plugin : this.plugins.values()) {
				plugin.postProcessModule(module);
			}
		}
	}

	private void removeModule(Module module) {
		if (this.plugins != null) {
			for (Plugin plugin : this.plugins.values()) {
				plugin.removeModule(module);
			}
		}
	}

	private void launchModule(Module module, Map<String, String> parameters) {
		if (this.plugins != null) {
			for (Plugin plugin : this.plugins.values()) {
				// Currently, launching module is applicable only to Jobs
				if (plugin instanceof JobPlugin) {
					((JobPlugin) plugin).launch(module, parameters);
				}
			}
		}
	}

	private void beforeShutdown(Module module) {
		if (this.plugins != null) {
			for (Plugin plugin : this.plugins.values()) {
				plugin.beforeShutdown(module);
			}
		}
	}

	private void fireModuleDeployedEvent(Module module) {
		if (this.eventPublisher != null) {
			ModuleDeployedEvent event = new ModuleDeployedEvent(module, this.deployerContext.getId());
			event.setAttribute("group", module.getDeploymentMetadata().getGroup());
			event.setAttribute("index", "" + module.getDeploymentMetadata().getIndex());
			this.eventPublisher.publishEvent(event);
		}
	}

	private void fireModuleUndeployedEvent(Module module) {
		if (this.eventPublisher != null) {
			ModuleUndeployedEvent event = new ModuleUndeployedEvent(module, this.deployerContext.getId());
			event.setAttribute("group", module.getDeploymentMetadata().getGroup());
			event.setAttribute("index", "" + module.getDeploymentMetadata().getIndex());
			this.eventPublisher.publishEvent(event);
		}
	}

}
