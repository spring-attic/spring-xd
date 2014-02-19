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

package org.springframework.xd.module.core;

import java.util.Properties;

import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.core.io.Resource;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.ModuleType;

/**
 * @author Mark Fisher
 * @author David Turanski
 * @author Gary Russell
 */
public interface Module extends Lifecycle {

	void initialize();

	/**
	 * @return the generic module name or template name
	 */
	String getName();

	ModuleType getType();

	DeploymentMetadata getDeploymentMetadata();

	void setParentContext(ApplicationContext parentContext);

	void addComponents(Resource resource);

	void addProperties(Properties properties);

	Properties getProperties();

	<T> T getComponent(Class<T> requiredType);

	<T> T getComponent(String componentName, Class<T> requiredType);

	/**
	 * Destroy this module's application context.
	 */
	void destroy();

}
