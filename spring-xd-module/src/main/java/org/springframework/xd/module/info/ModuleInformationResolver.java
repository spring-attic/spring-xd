/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.module.info;

import org.springframework.xd.module.ModuleDefinition;

/**
 * To be implemented by services able to derive {@link org.springframework.xd.module.info.ModuleInformation} out of a
 * {@link org.springframework.xd.module.ModuleDefinition}.
 *
 * @author Eric Bottard
 */
public interface ModuleInformationResolver {

	/**
	 * Return information about the given module, or {@code null} if the provided definition is not supported by this
	 * resolver.
	 */
	public ModuleInformation resolve(ModuleDefinition definition);
}
