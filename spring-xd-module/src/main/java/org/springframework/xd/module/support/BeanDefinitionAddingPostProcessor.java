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

package org.springframework.xd.module.support;

import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Implementation of {@link BeanDefinitionRegistryPostProcessor} that adds all beans defined in the specified
 * {@link Resource}s. Used by plugins to hook into the shared application context.
 * 
 * Alternatively, allows for adding {@link BeanDefinition}s programmatically as well.
 * 
 * @author Jennifer Hickey
 * @author Gunnar Hillert
 * @author David Turanski
 * 
 * @since 1.0
 * 
 */
public class BeanDefinitionAddingPostProcessor implements BeanDefinitionRegistryPostProcessor {

	private final Resource[] resources;

	private final ConfigurableEnvironment environment;

	private final SortedMap<String, BeanDefinition> beanDefinitions = new TreeMap<String, BeanDefinition>();

	public BeanDefinitionAddingPostProcessor(ConfigurableEnvironment environment, Resource... resources) {
		this.environment = environment;
		this.resources = resources;
	}

	public BeanDefinitionAddingPostProcessor() {
		this.resources = new Resource[] {};
		this.environment = null;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
		propagateXdProperties(reader);
		reader.loadBeanDefinitions(resources);

		for (Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
			registry.registerBeanDefinition(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Allows you to add custom {@link BeanDefinition}s.
	 * 
	 * @param beanName The name of the bean instance to register
	 * @param beanDefinition Definition of the bean instance to register
	 * @return The BeanDefinitionAddingPostProcessor
	 */
	public BeanDefinitionAddingPostProcessor addBeanDefinition(String beanName, BeanDefinition beanDefinition) {

		Assert.hasText(beanName, "Bean name must not be empty");
		Assert.notNull(beanDefinition, "BeanDefinition must not be null");

		this.beanDefinitions.put(beanName, beanDefinition);
		return this;

	}

	private void propagateXdProperties(XmlBeanDefinitionReader reader) {
		// TODO: This code should be in OptionUtils, but can't create a circular dependency here
		if (this.environment != null) {
			reader.setEnvironment(this.environment);
		}
	}
}
