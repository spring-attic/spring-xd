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

import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public interface Plugin {

	/**
	 * Apply changes to the module before it is initialized (generally,
	 * this means before it's context is refreshed).
	 * @param module
	 */
	void preProcessModule(Module module);

	/**
	 * Apply changes to the module after it is initialized (generally,
	 * this means after it's context is refreshed), but before it is
	 * started.
	 * @param module
	 */
	void postProcessModule(Module module);

	/**
	 * Take any actions necessary to remove a module from the system.
	 * @param module
	 */
	void removeModule(Module module);

	/**
	 * Make any necessary changes to the shared context.
	 * @param context
	 */
	void postProcessSharedContext(ConfigurableApplicationContext context);

}
