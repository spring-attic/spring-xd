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


import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;
import org.springframework.xd.module.support.BeanDefinitionAddingPostProcessor;

/**
 * @author Glenn Renfro
 * @author Gary Russell
 */
public abstract class AbstractPlugin implements Plugin {

	protected final Log logger = LogFactory.getLog(this.getClass());

	/**
	 * Represents the path and file name of the context to be used in post processing of the module. If not set the
	 * postProcessContext will use defaults. Beans will be added to the shared context.
	 * 
	 */
	private String[] postProcessContextPaths;

	protected void setPostProcessContextPaths(String... postProcessContextPaths) {
		this.postProcessContextPaths = postProcessContextPaths;
	}

	/**
	 * Process the {@link Module} and add the Application Context resources necessary to setup the Batch Job.
	 **/
	@Override
	public final void preProcessModule(Module module) {
		Assert.notNull(module, "module cannot be null");
		List<String> componentPaths = componentPathsSelector(module);
		for (String path : componentPaths) {
			addComponents(module, path);
		}
		configureProperties(module);
		this.preProcessModuleInternal(module);
	}

	/**
	 * Perform any plugin-specific pre-refresh initialization.
	 * 
	 * @param module
	 */
	protected void preProcessModuleInternal(Module module) {
	}


	/**
	 * Perform any plugin-specific post-refresh initialization.
	 * 
	 * @param module
	 */
	protected void postProcessModuleInternal(Module module) {
	}

	/**
	 * Establish the configuration file path and names required to setup the context for the type of module you are
	 * deploying.
	 * 
	 * @param module The module that is being initialized
	 * @return The list of paths
	 */
	protected abstract List<String> componentPathsSelector(Module module);

	/**
	 * set the properties required for the module based on its type.
	 * 
	 * @param module The module that is being initialized
	 */
	protected abstract void configureProperties(Module module);

	@Override
	public void preProcessSharedContext(ConfigurableApplicationContext context) {
		if (postProcessContextPaths != null) {
			addBeanFactoryPostProcessors(context, postProcessContextPaths);
		}
	}


	private void addComponents(Module module, String path) {
		module.addComponents(new ClassPathResource(path));
	}

	private void addBeanFactoryPostProcessors(ConfigurableApplicationContext context, String... paths) {
		for (String path : paths) {
			context.addBeanFactoryPostProcessor(new BeanDefinitionAddingPostProcessor(context.getEnvironment(), new ClassPathResource(path)));
		}
	}

}
