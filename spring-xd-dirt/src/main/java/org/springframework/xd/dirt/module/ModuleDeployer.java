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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindException;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.dirt.event.ModuleDeployedEvent;
import org.springframework.xd.dirt.event.ModuleUndeployedEvent;
import org.springframework.xd.dirt.plugins.job.JobPlugin;
import org.springframework.xd.module.CompositeModule;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.Module;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.ParentLastURLClassLoader;
import org.springframework.xd.module.Plugin;
import org.springframework.xd.module.SimpleModule;
import org.springframework.xd.module.options.DefaultModuleOptionsMetadata;
import org.springframework.xd.module.options.ModuleOptions;
import org.springframework.xd.module.options.ModuleOptionsMetadata;

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

	private volatile ConfigurableApplicationContext commonContext;

	private volatile ApplicationEventPublisher eventPublisher;

	private final ObjectMapper mapper = new ObjectMapper();

	private final ConcurrentMap<String, Map<Integer, Module>> deployedModules = new ConcurrentHashMap<String, Map<Integer, Module>>();

	private volatile Map<String, Plugin> plugins;

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
		this.deployerContext = context;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void onInit() {
		this.plugins = this.deployerContext.getBeansOfType(Plugin.class);
		SpringApplicationBuilder application = new SpringApplicationBuilder(XDContainer.XD_INTERNAL_CONFIG_ROOT
				+ "module-common.xml", PropertyPlaceholderAutoConfiguration.class).web(false);
		application.application().setShowBanner(false);
		ConfigurableApplicationContext globalContext = (ConfigurableApplicationContext) deployerContext.getParent();
		application.parent(globalContext);
		if (globalContext != null) {
			application.environment(globalContext.getEnvironment());
		}
		application.initializers(new ApplicationContextInitializer<ConfigurableApplicationContext>() {

			@Override
			public void initialize(ConfigurableApplicationContext applicationContext) {
				for (Plugin plugin : plugins.values()) {
					plugin.preProcessSharedContext(applicationContext);
				}
			};
		});
		this.commonContext = application.run();
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.parentClassLoader = classLoader;
	}

	@Override
	protected synchronized void handleMessageInternal(Message<?> message) throws Exception {
		String payloadString = message.getPayload().toString();
		ModuleDeploymentRequest deserialized = this.mapper.readValue(payloadString, ModuleDeploymentRequest.class);
		if (deserialized instanceof CompositeModuleDeploymentRequest) {
			handleCompositeModuleMessage((CompositeModuleDeploymentRequest) deserialized);
		}
		else {
			handleSingleModuleMessage(deserialized);
		}
	}

	private void handleCompositeModuleMessage(CompositeModuleDeploymentRequest request) {
		List<ModuleDeploymentRequest> children = request.getChildren();
		Assert.notEmpty(children, "child module list must not be empty");
		List<ModuleDefinition> definitions = new ArrayList<ModuleDefinition>();
		List<Map<String, String>> paramList = new ArrayList<Map<String, String>>();
		for (ModuleDeploymentRequest child : children) {
			String name = child.getModule();
			ModuleType type = child.getType();
			ModuleDefinition definition = this.moduleDefinitionRepository.findByNameAndType(name, type);
			Assert.notNull(definition, "No moduleDefinition for " + type + ":" + name);
			definitions.add(definition);
			Map<String, String> params = child.getParameters();
			if (CollectionUtils.isEmpty(params)) {
				params = Collections.emptyMap();
			}
			paramList.add(params);
		}
		String group = request.getGroup();
		int index = request.getIndex();
		String sourceChannelName = request.getSourceChannelName();
		String sinkChannelName = request.getSinkChannelName();
		DeploymentMetadata metadata = new DeploymentMetadata(group, index, sourceChannelName, sinkChannelName);
		List<SimpleModule> modules = new ArrayList<SimpleModule>(definitions.size());
		if (parentClassLoader == null) {
			parentClassLoader = ClassUtils.getDefaultClassLoader();
		}
		for (int i = 0; i < definitions.size(); i++) {
			ModuleDefinition definition = definitions.get(i);
			DeploymentMetadata submoduleMetadata = new DeploymentMetadata(metadata.getGroup() + "."
					+ request.getModule(), i);
			ClassLoader classLoader = (definition.getClasspath() == null) ? null
					: new ParentLastURLClassLoader(definition.getClasspath(), parentClassLoader);

			DefaultModuleOptionsMetadata moduleOptionsMetadata = new DefaultModuleOptionsMetadata();
			ModuleOptions subModuleOptions = safeModuleOptionsInterpolate(moduleOptionsMetadata, paramList.get(i));
			SimpleModule module = new SimpleModule(definition, submoduleMetadata, classLoader, subModuleOptions);

			Properties props = new Properties();
			props.putAll(paramList.get(i));
			module.addProperties(props);
			if (logger.isDebugEnabled()) {
				logger.debug("added properties for child module [" + module.getName() + "]: " + props);
			}

			modules.add(module);
		}
		CompositeModule module = new CompositeModule(request.getModule(), request.getType(), modules, metadata);
		deployAndStore(module, request);
	}

	private ModuleOptions safeModuleOptionsInterpolate(ModuleOptionsMetadata moduleOptionsMetadata,
			Map<String, String> values) {
		try {
			return moduleOptionsMetadata.interpolate(values);
		}
		catch (BindException e) {
			// Can't happen as parser should have already validated options
			throw new IllegalStateException(e);
		}
	}

	private void handleSingleModuleMessage(ModuleDeploymentRequest request) {
		if (request.isRemove()) {
			handleUndeploy(request);
		}
		else if (request.isLaunch()) {
			handleLaunch(request);
		}
		else {
			handleDeploy(request);
		}
	}

	private void handleDeploy(ModuleDeploymentRequest request) {
		String group = request.getGroup();
		int index = request.getIndex();
		String name = request.getModule();
		ModuleType type = request.getType();
		ModuleDefinition definition = this.moduleDefinitionRepository.findByNameAndType(name, type);
		Assert.notNull(definition, "No moduleDefinition for " + name + ":" + type);
		DeploymentMetadata metadata = new DeploymentMetadata(group, index, request.getSourceChannelName(),
				request.getSinkChannelName());

		ClassLoader classLoader = (definition.getClasspath() == null) ? null
				: new ParentLastURLClassLoader(definition.getClasspath(), parentClassLoader);


		Map<String, String> parameters = request.getParameters();
		ModuleOptionsMetadata moduleOptionsMetadata = definition.getModuleOptionsMetadata();
		ModuleOptions interpolated = safeModuleOptionsInterpolate(moduleOptionsMetadata, parameters);
		Module module = new SimpleModule(definition, metadata, classLoader, interpolated);
		this.deployAndStore(module, request);
	}

	private void deployAndStore(Module module, ModuleDeploymentRequest request) {
		module.setParentContext(this.commonContext);
		this.deploy(module);
		if (logger.isInfoEnabled()) {
			logger.info("deployed " + module.toString());
		}
		this.deployedModules.putIfAbsent(request.getGroup(), new HashMap<Integer, Module>());
		this.deployedModules.get(request.getGroup()).put(request.getIndex(), module);
	}

	private void deploy(Module module) {
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

	private void handleLaunch(ModuleDeploymentRequest request) {
		String group = request.getGroup();
		Map<Integer, Module> modules = this.deployedModules.get(group);
		if (modules != null) {
			processLaunchRequest(modules, request);
		}
		else {
			// Deploy the job module and then launch
			handleDeploy(request);
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
