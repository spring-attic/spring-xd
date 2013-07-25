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


import java.util.List;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;

/**
 * @author Glenn Renfro
 * @author Gary Russell
 */
public abstract class AbstractPlugin implements Plugin{

	/**
	 * Represents the path and file name of the context to be used in post
	 * processing of the module. If not set the postProcessContext will use
	 * defaults.
	 *
	 */
	protected String postProcessContextPath;

	/**
	 * Process the {@link Module} and add the Application Context resources
	 * necessary to setup the Batch Job.
	 **/
	@Override
	public final void preProcessModule(Module module) {
		Assert.notNull(module, "module cannot be null");
		List<String> componentPaths = componentPathsSelector(module);
		for(String path: componentPaths) {
			addComponents(module, path);
			configureProperties(module);
		}
		this.preProcessModuleInternal(module);
	}

	/**
	 * Perform any plugin-specific pre-refresh initialization.
	 * @param module
	 */
	protected void preProcessModuleInternal(Module module) {
	}



	@Override
	public final void postProcessModule(Module module) {
		this.postProcessModuleInternal(module);
	}

	/**
	 * Perform any plugin-specific post-refresh initialization.
	 * @param module
	 */
	protected void postProcessModuleInternal(Module module) {
	}

	/**
	 * Establish the configuration file path and names required to setup the context for the
	 * type of module you are deploying.
	 * @param module The module that is being initialized
	 * @param group The group the module belongs
	 * @param index The offset of the module in the stream
	 * @return
	 */
	protected abstract List<String> componentPathsSelector(Module module);

	/**
	 * set the properties required for the module based on its type.
	 * @param module The module that is being initialized
	 * @param group The group the module belongs
	 * @param index The offset of the module in the stream
	 */
	protected abstract void configureProperties(Module module);

	@Override
	public void postProcessSharedContext(ConfigurableApplicationContext context){
		if(postProcessContextPath != null){
			addBeanFactoryPostProcessor(context, postProcessContextPath);
		}
	}

	@Override
	public void removeModule(Module module) {
	}

	private void addComponents(Module module, String path){
		module.addComponents(new ClassPathResource(path));
	}

	private void addBeanFactoryPostProcessor(ConfigurableApplicationContext context, String path) {
		context.addBeanFactoryPostProcessor(new BeanDefinitionAddingPostProcessor(new ClassPathResource(path)));
	}

}
