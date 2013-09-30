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

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.initializer.ContextIdApplicationContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * A {@link Module} implementation backed by a Spring {@link ApplicationContext}.
 * 
 * @author Mark Fisher
 * @author David Turanski
 * @author Gary Russell
 * @author Dave Syer
 */
public class SpringApplicationModule extends AbstractModule {

	private final Log logger = LogFactory.getLog(this.getClass());

	private ConfigurableApplicationContext context;

	private final ConfigurableEnvironment environment;

	private final SpringApplicationBuilder application;

	private final AtomicInteger propertiesCounter = new AtomicInteger();

	private final Properties properties = new Properties();

	public SpringApplicationModule(ModuleDefinition definition, DeploymentMetadata metadata) {
		this(definition, metadata, null);
	}

	public SpringApplicationModule(ModuleDefinition definition, DeploymentMetadata metadata, ClassLoader classLoader) {
		super(definition, metadata);
		application = new SpringApplicationBuilder().sources(PropertyPlaceholderAutoConfiguration.class).web(false);
		// How does HTTP source work?;
		environment = new StandardEnvironment();
		if (classLoader != null) {
			application.resourceLoader(new PathMatchingResourcePatternResolver(classLoader));
		}
		if (definition != null) {
			if (definition.getResource().isReadable()) {
				this.addComponents(definition.getResource());
			}
			if (definition.getProperties() != null) {
				this.addProperties(definition.getProperties());
			}
		}
	}


	@Override
	public void setParentContext(ApplicationContext parent) {
		this.application.parent((ConfigurableApplicationContext) parent);
	}

	@Override
	public void addComponents(Resource resource) {
		addSource(resource);
	}

	protected void addSource(Object source) {
		application.sources(source);
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
		this.environment.getPropertySources().addLast(propertySource);
	}

	@Override
	public void initialize() {
		this.application.initializers(new ContextIdApplicationContextInitializer(this.toString()));
		this.application.environment(environment);
		this.context = this.application.run();
		if (logger.isInfoEnabled()) {
			logger.info("initialized module: " + this.toString());
		}
	}

	@Override
	public void start() {
		context.start();
	}

	@Override
	public void stop() {
		context.stop(); // Shouldn't need to close() as well?
	}

	@Override
	public boolean isRunning() {
		return context.isRunning();
	}

	@Override
	public void destroy() {
		if (context instanceof DisposableBean) {
			try {
				((DisposableBean) context).destroy();
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
	}
}
