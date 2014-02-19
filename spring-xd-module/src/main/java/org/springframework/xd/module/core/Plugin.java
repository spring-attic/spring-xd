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

import org.springframework.context.ConfigurableApplicationContext;

/**
 * To be implemented by classes that want to alter how a {@link Module} works. Typically, implementations will add
 * behavior to the module by {@link Module#addComponents(org.springframework.core.io.Resource) registering additional
 * components} or {@link Module#addProperties(java.util.Properties) setting additional properties}.
 * 
 * @see ModuleDeployer
 * 
 * @author Mark Fisher
 * @author Gary Russell
 * @author Eric Bottard
 */
public interface Plugin {

	/**
	 * Apply changes to the module before it is initialized <i>i.e.</i> before its context is refreshed.
	 */
	void preProcessModule(Module module);

	/**
	 * Apply changes to the module after it is initialized <i>i.e.</i> after its context is refreshed, but before it is
	 * started.
	 */
	void postProcessModule(Module module);

	/**
	 * Take any actions necessary to remove a module from the system.
	 * 
	 * @param module
	 */
	void removeModule(Module module);


	/**
	 * Perform any cleanup necessary prior to module shutdown
	 * 
	 * @param module
	 */
	void beforeShutdown(Module module);

	/**
	 * Invoked when this plugin is discovered, allows to make any necessary changes to the context which will be used as
	 * the parent of all {@link Module#setParentContext(org.springframework.context.ApplicationContext) modules}. Note
	 * that said context has not been {@link ConfigurableApplicationContext#refresh() refreshed} yet.
	 */
	void preProcessSharedContext(ConfigurableApplicationContext context);

	/**
	 * Check if the module is supported by the plugin.
	 * 
	 * @param module
	 */
	boolean supports(Module module);

}
