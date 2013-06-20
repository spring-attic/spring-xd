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

/**
 * @author Mark Fisher
 */
public class SimpleModule extends AbstractModule {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final GenericApplicationContext context = new GenericApplicationContext();

	private final AtomicInteger propertiesCounter = new AtomicInteger();

	private final Properties properties = new Properties();


	public SimpleModule(String name, String type) {
		super(name, type);
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

	/*
	 * Lifecycle implementation
	 */

	@Override
	public void start() {
		if (!this.isRunning()) {
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

}
