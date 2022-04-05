/*
 *
 *  * Copyright 2011-2014 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      https://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.xd.dirt.module;

import org.springframework.xd.module.SimpleModuleDefinition;

/**
 * A strategy interface for deciding whether a target definition is stale compared to a source definition.
 *
 * @see org.springframework.xd.dirt.module.SynchronizingModuleRegistry
 * @since 1.2
 * @author Eric Bottard
 */
public interface StalenessCheck {

	/**
	 * Return whether the target definition (which may be {@code null}) is stale compared to the source definition.
	 */
	boolean isStale(SimpleModuleDefinition targetDefinition, SimpleModuleDefinition sourceDefinition);
}
