/*
 * Copyright 2013-2015 the original author or authors.
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


/**
 * To be implemented by classes that want to alter how a {@link Module} works. Typically, implementations will add
 * behavior to the module by registering additional
 * components or {@link Module#addProperties(java.util.Properties) setting additional properties}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Eric Bottard
 * @author David Turanski
 */
public interface Plugin {

	/**
	 * Apply changes to the module before it is initialized <i>i.e.</i> before its context is refreshed.
	 *
	 * @param module the module to pre-process
	 */
	void preProcessModule(Module module);

	/**
	 * Apply changes to the module after it is initialized <i>i.e.</i> after its context is refreshed, but before it is
	 * started.
	 *
	 * @param module the module to post-process
	 */
	void postProcessModule(Module module);

	/**
	 * Take any actions necessary to remove a module from the system.
	 *
	 * @param module the module to remove
	 */
	void removeModule(Module module);

	/**
	 * Perform any cleanup necessary prior to module shutdown
	 *
	 * @param module the module to cleanup
	 */
	void beforeShutdown(Module module);

	/**
	 * Check if the module is supported by the plugin.
	 *
	 * @param module the module to check for support
	 */
	boolean supports(Module module);

}
