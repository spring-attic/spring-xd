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

package org.springframework.xd.module;

import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * A {@link Module} implementation backed by a Spring {@link ApplicationContext}.
 * 
 * @author Mark Fisher
 * @author David Turanski
 * @author Gary Russell
 */
public class SimpleModule extends AbstractModule {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final GenericApplicationContext context;

	private final AtomicInteger propertiesCounter = new AtomicInteger();

	private final Properties properties = new Properties();

	private final AtomicBoolean isRunning = new AtomicBoolean();


	public SimpleModule(ModuleDefinition definition, DeploymentMetadata metadata) {
		this(definition, metadata, null);
	}

	public SimpleModule(ModuleDefinition definition, DeploymentMetadata metadata, ClassLoader classLoader) {
		super(definition, metadata);
		context = new GenericApplicationContext();
		if (classLoader != null) {
			context.setClassLoader(classLoader);
		}
		if (definition != null) {
			if (definition.getProperties() != null) {
				this.addProperties(definition.getProperties());
			}
		}
	}


	@Override
	public void setParentContext(ApplicationContext parent) {
		this.context.setParent(parent);
	}

	@Override
	public void addComponents(Resource resource) {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.context);
		reader.loadBeanDefinitions(resource);
	}

	@Override
	public void addProperties(Properties properties) {
		this.registerPropertySource(properties);
		this.properties.putAll(properties);
		this.activateProfiles(properties);
	}

	/**
	 * Activates Spring profiles for each option explicitly set on this Module.
	 * 
	 * <p>
	 * This allows module implementors to radically change the behavior of the module given an option set, thus
	 * alleviating the creation of two distinct modules. Obviously, this should not be abused and is useful for
	 * module(s) that still provide related functionality.
	 * <p>
	 * 
	 * <p>
	 * More specifically, for each explicit {@link Module#getProperties() property} ({@code key = value}) set, the
	 * following profiles will be activated:
	 * <ul>
	 * <li>{@literal -profile-[key]}: can be used to include beans as soon as the user provided the option,</li>
	 * <li>{@literal -profile-[key]-[value]}: used to include beans when the user provided a specific value for the
	 * option</li>
	 * </ul>
	 **/
	private void activateProfiles(Properties properties) {
		Set<Entry<Object, Object>> entrySet = properties.entrySet();
		ConfigurableEnvironment environment = context.getEnvironment();
		for (Entry<Object, Object> entry : entrySet) {
			environment.addActiveProfile("-profile-" + String.valueOf(entry.getKey()));
			environment.addActiveProfile("-profile-" + String.valueOf(entry.getKey()) + "-"
					+ String.valueOf(entry.getValue()));
		}
	}

	@Override
	public Properties getProperties() {
		return this.properties;
	}

	public ApplicationContext getApplicationContext() {
		return this.context;
	}

	@Override
	public <T> T getComponent(Class<T> requiredType) {
		return this.context.getBean(requiredType);
	}

	@Override
	public <T> T getComponent(String componentName, Class<T> requiredType) {
		if (this.context.containsBean(componentName)) {
			return context.getBean(componentName, requiredType);
		}
		return null;
	}

	private void registerPropertySource(Properties properties) {
		int propertiesIndex = this.propertiesCounter.getAndIncrement();
		String propertySourceName = "properties-" + propertiesIndex;
		PropertySource<?> propertySource = new PropertiesPropertySource(propertySourceName, properties);
		this.context.getEnvironment().getPropertySources().addLast(propertySource);
	}

	@Override
	public void initialize() {
		Assert.state(this.context != null, "An ApplicationContext is required");
		boolean propertyConfigurerPresent = false;
		if (definition.getResource() != null) {
			this.addComponents(definition.getResource());
		}
		for (String name : this.context.getBeanDefinitionNames()) {
			if (name.startsWith("org.springframework.context.support.PropertySourcesPlaceholderConfigurer")) {
				propertyConfigurerPresent = true;
				break;
			}
		}
		if (!propertyConfigurerPresent) {
			PropertySourcesPlaceholderConfigurer placeholderConfigurer = new PropertySourcesPlaceholderConfigurer();
			placeholderConfigurer.setEnvironment(this.context.getEnvironment());
			this.context.addBeanFactoryPostProcessor(placeholderConfigurer);
		}
		this.context.setId(this.toString());
		this.context.refresh();
		if (logger.isInfoEnabled()) {
			logger.info("initialized module: " + this.toString());
		}
	}

	@Override
	public void destroy() {
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
			this.context.start();
			if (logger.isInfoEnabled()) {
				logger.info("started module: " + this.toString());
			}
		}
	}

	@Override
	public void stop() {
		if (this.isRunning.compareAndSet(true, false)) {
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
