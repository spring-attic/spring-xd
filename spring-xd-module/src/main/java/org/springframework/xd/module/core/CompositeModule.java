/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.module.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;

/**
 * @author Mark Fisher
 * @author David Turanski
 * @author Gary Russell
 */
public class CompositeModule extends AbstractModule {

	public static final String OPTION_SEPARATOR = ".";

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final GenericApplicationContext context = new GenericApplicationContext();

	private final List<Module> modules;

	private final Properties properties = new Properties();

	private final AtomicInteger propertiesCounter = new AtomicInteger();

	private final AtomicBoolean isRunning = new AtomicBoolean();

	public CompositeModule(ModuleDescriptor descriptor, ModuleDeploymentProperties deploymentProperties,
			List<Module> modules) {
		super(descriptor, deploymentProperties);
		this.modules = modules;
		this.validate();
	}

	//TODO: This is specific to XD stream composition. Eventually we may want to support more generic composite modules.
	private void validate() {
		Assert.isTrue(modules != null && modules.size() > 0, "at least one module required");
		ModuleType inferredType = null;
		if (modules.size() == 1) {
			inferredType = modules.get(0).getType();
		}
		else {
			ModuleType firstType = modules.get(0).getType();
			ModuleType lastType = modules.get(modules.size() - 1).getType();
			boolean hasInput = firstType != ModuleType.source;
			boolean hasOutput = lastType != ModuleType.sink;
			if (hasInput && hasOutput) {
				inferredType = ModuleType.processor;
			}
			else if (hasInput) {
				inferredType = ModuleType.sink;
			}
			else if (hasOutput) {
				inferredType = ModuleType.source;
			}
		}
		Assert.isTrue(inferredType == this.getType(),
				"invalid composite module: inferred type=" + inferredType + ", stated type=" + this.getType());
	}

	@Override
	public void initialize() {
		List<AbstractEndpoint> endpoints = new ArrayList<AbstractEndpoint>();
		MessageChannel previousOutputChannel = null;
		for (int i = 0; i < this.modules.size(); i++) {
			Module module = this.modules.get(i);
			module.initialize();
			MessageChannel inputChannel = module.getComponent("input", MessageChannel.class);
			MessageChannel outputChannel = module.getComponent("output", MessageChannel.class);
			if (i == 0 && inputChannel != null) {
				// this will act as THE input for the composite module
				// if the first module has no input, the composite is a source
				this.context.getBeanFactory().registerSingleton("input", inputChannel);
			}
			if (i > 0) {
				// first module MAY have 'input', all others MUST
				Assert.notNull(inputChannel, "each module after the first must provide 'input'");
			}
			if (previousOutputChannel != null) {
				BridgeHandler handler = new BridgeHandler();
				handler.setBeanFactory(this.context.getBeanFactory());
				handler.setOutputChannel(inputChannel);
				handler.afterPropertiesSet();
				ConsumerEndpointFactoryBean bridgeFactoryBean = new ConsumerEndpointFactoryBean();
				bridgeFactoryBean.setInputChannel(previousOutputChannel);
				bridgeFactoryBean.setHandler(handler);
				try {
					// TODO: might not be necessary to pass this context, but the FB requires non-null
					bridgeFactoryBean.setBeanFactory(this.context.getBeanFactory());
					String beanName = "bridge-" + i;
					bridgeFactoryBean.setBeanName(beanName); // avoid debug (error since 4.2.1) log from CEFB
					bridgeFactoryBean.afterPropertiesSet();
					AbstractEndpoint endpoint = bridgeFactoryBean.getObject();
					endpoints.add(endpoint);
					this.context.getBeanFactory().registerSingleton(beanName, endpoint);
					endpoint.setComponentName(beanName);
					endpoint.afterPropertiesSet();
				}
				catch (Exception e) {
					throw new IllegalStateException("failed to start bridge for CompositeModule", e);
				}
			}
			if (i < this.modules.size() - 1) {
				// last module MAY have 'output', all others MUST
				Assert.notNull(outputChannel, "each module before the last must provide 'output'");
			}
			previousOutputChannel = outputChannel;
			if (i == this.modules.size() - 1 && outputChannel != null) {
				// this will act as THE output for the composite module
				// if the final module has no outputChannel, the composite is a sink
				this.context.getBeanFactory().registerSingleton("output", outputChannel);
			}
		}
		for (int i = endpoints.size() - 1; i >= 0; i--) {
			endpoints.get(i).start();
		}
		initContext();
		if (logger.isInfoEnabled()) {
			logger.info("initialized module: " + this.toString());
		}
	}

	@Override
	public ConfigurableApplicationContext getApplicationContext() {
		return context;
	}

	private void initContext() {
		Assert.state(context != null, "An ApplicationContext is required");
		boolean propertyConfigurerPresent = false;
		for (String name : context.getBeanDefinitionNames()) {
			if (name.startsWith("org.springframework.context.support.PropertySourcesPlaceholderConfigurer")) {
				propertyConfigurerPresent = true;
				break;
			}
		}
		if (!propertyConfigurerPresent) {
			PropertySourcesPlaceholderConfigurer placeholderConfigurer = new PropertySourcesPlaceholderConfigurer();
			placeholderConfigurer.setEnvironment(context.getEnvironment());
			context.addBeanFactoryPostProcessor(placeholderConfigurer);
		}
		context.setId(this.toString());
		context.refresh();
	}

	@Override
	public void setParentContext(ApplicationContext parentContext) {
		this.context.setParent(parentContext);
		for (Module module : modules) {
			module.setParentContext(parentContext);
		}
	}

	@Override
	public void addListener(ApplicationListener<?> listener) {
		for (Module module : modules) {
			module.addListener(listener);
		}
	}

	@Override
	public void addSource(Object source) {
		Assert.notNull(source, "source cannot be null");
		Assert.isInstanceOf(Resource.class, source, "unsupported source: " + source.getClass().getName());
		Resource resource = (Resource) source;
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.context);
		reader.loadBeanDefinitions(resource);
	}

	@Override
	public void addProperties(Properties properties) {
		this.registerPropertySource(properties);
		this.properties.putAll(properties);
		for (Module module : this.modules) {
			module.addProperties(properties);
		}
	}

	private void registerPropertySource(Properties properties) {
		int propertiesIndex = this.propertiesCounter.getAndIncrement();
		String propertySourceName = "properties-" + propertiesIndex;
		PropertySource<?> propertySource = new PropertiesPropertySource(propertySourceName, properties);
		this.context.getEnvironment().getPropertySources().addLast(propertySource);
	}

	@Override
	public Properties getProperties() {
		return this.properties;
	}

	@Override
	public <T> T getComponent(Class<T> requiredType) {
		return (this.context.isActive()) ? this.context.getBean(requiredType) : null;
	}

	@Override
	public <T> T getComponent(String componentName, Class<T> requiredType) {
		if (this.context.isActive() && this.context.containsBean(componentName)) {
			return context.getBean(componentName, requiredType);
		}
		return null;
	}

	@Override
	public void destroy() {
		for (Module module : this.modules) {
			module.destroy();
		}
		if (this.context != null) {
			this.context.destroy();
		}
	}

	/*
	 * Lifecycle implementation
	 */

	@Override
	public void start() {
		Assert.state(this.context != null, "An ApplicationContext is required");
		if (this.isRunning.compareAndSet(false, true)) {
			for (int i = this.modules.size() - 1; i >= 0; i--) {
				Module module = this.modules.get(i);
				module.start();
			}
			try {
				this.context.start();
			}
			catch (BeansException be) {
				this.context.destroy();
				throw be;
			}
			if (logger.isInfoEnabled()) {
				logger.info("started module: " + this.toString());
			}
		}
	}

	@Override
	public void stop() {
		if (this.isRunning.compareAndSet(true, false)) {
			for (Module module : this.modules) {
				module.stop();
			}
			this.context.stop();
			if (logger.isInfoEnabled()) {
				logger.info("stopped module: " + this.toString());
			}
		}
	}

	@Override
	public boolean isRunning() {
		return isRunning.get();
	}


}
