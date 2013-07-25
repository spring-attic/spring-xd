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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author Mark Fisher
 * @author David Turanski
 * @author Gary Russell
 */
public class SimpleModule extends AbstractModule {

	private final static String MEDIA_TYPE_BEAN_NAME = "accepted-media-types";

	private final Log logger = LogFactory.getLog(this.getClass());

	private final GenericApplicationContext context = new GenericApplicationContext();

	private final AtomicInteger propertiesCounter = new AtomicInteger();

	private final Properties properties = new Properties();


	public SimpleModule(ModuleDefinition definition, DeploymentMetadata metadata) {
		super(definition, metadata);
		if (definition != null) {
			if (definition.getResource() != null) {
				this.addComponents(definition.getResource());
			}
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
	}

	@Override
	public Properties getProperties() {
		return this.properties;
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
		this.context.refresh();
		if (logger.isInfoEnabled()) {
			logger.info("initialized module: " + this.toString());
		}
	}

	/*
	 * Lifecycle implementation
	 */

	@Override
	public void start() {
		Assert.state(this.context != null, "An ApplicationContext is required");
		if (!this.isRunning()) {
			this.context.start();
			if (logger.isInfoEnabled()) {
				logger.info("started module: " + this.toString());
			}
		}
	}

	@Override
	public void stop() {
		if (this.isRunning()) {
			this.context.stop();
			this.context.destroy();
			if (logger.isInfoEnabled()) {
				logger.info("stopped mod: " + this.toString());
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.context.isActive() && this.context.isRunning();
	}

	public ApplicationContext getApplicationContext() {
		return this.context;
	}


	@SuppressWarnings("unchecked")
	@Override
	public List<MediaType> getAcceptedMediaTypes() {
		//TODO: This should only apply to processors and sinks
		if (!this.context.containsBean(MEDIA_TYPE_BEAN_NAME)) {
			return Arrays.asList(MediaType.ALL);
		}
		List<String> acceptedTypes =  this.context.getBean(MEDIA_TYPE_BEAN_NAME,List.class);
		if (CollectionUtils.isEmpty(acceptedTypes)) {
			return Arrays.asList(MediaType.ALL);
		}
		List<MediaType> acceptedMediaTypes = new ArrayList<MediaType>(acceptedTypes.size());
		for (String acceptedType: acceptedTypes) {
			acceptedMediaTypes.add(MediaType.parseMediaType(acceptedType));
		}
		return Collections.unmodifiableList(acceptedMediaTypes);
	}

}
