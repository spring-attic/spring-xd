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

package org.springframework.xd.module.core;

import java.util.Properties;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;

/**
 * @author Mark Fisher
 * @author David Turanski
 * @author Gary Russell
 * @author Ilayaperumal Gopinathan
 */
public interface Module extends Lifecycle {

	void initialize();

	/**
	 * @return the generic module name or template name.
	 */
	String getName();

	/**
	 *
	 * @return the module type.
	 */
	ModuleType getType();

	/**
	 *
	 * @return the module descriptor.
	 */
	ModuleDescriptor getDescriptor();

	/**
	 *
	 * @return the module deployment properties
	 */
	ModuleDeploymentProperties getDeploymentProperties();

	/**
	 *
	 * @return the module's top level application context
	 */
	ConfigurableApplicationContext getApplicationContext();

	//TODO:  is this still needed?

	/**
	 * set a parent application context
	 * @param parentContext
	 */
	void setParentContext(ApplicationContext parentContext);

	/**
	 * @see org.springframework.boot.builder.SpringApplicationBuilder#sources
	 * @param source can be a configuration class, bean definition {@link org.springframework.core.io.Resource}
	 * (e.g. XML or groovy file), or an annotated component, or an array of such objects.
	 */
	void addSource(Object source);

	/**
	 * Add properties to the environment.
	 * @param properties
	 */
	void addProperties(Properties properties);

	/**
	 * Add an application listener to the application context.
	 * @param listener the listener
	 */
	void addListener(ApplicationListener<?> listener);

	/**
	 * Get the module's properties.
	 * @return the properties
	 */
	Properties getProperties();

	/**
	 * Get a bean instance by its class.
	 * @param requiredType the class of the target bean
	 * @return the bean
	 */
	<T> T getComponent(Class<T> requiredType);

	/**
	 * Get a bean instance by its name and class.
	 * @param componentName the name of the target bean
	 * @param requiredType the class of the target bean
	 * @return the bean
	 */
	<T> T getComponent(String componentName, Class<T> requiredType);

	/**
	 * Destroy this module's application context.
	 */
	void destroy();

	/**
	 * Should the module require messagebus binding.
	 */
	boolean shouldBind();

}
