/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.module.options;

import java.util.Map;

import org.springframework.validation.BindException;


/**
 * Encapsulates metadata about the accepted options for a module.
 * 
 * @author Eric Bottard
 */
public interface ModuleOptionsMetadata extends Iterable<ModuleOption> {

	/**
	 * Derive information about the module behavior once it is part of a stream and user provided values for the module
	 * options are known.
	 * 
	 * @param raw the user provided options (from the DSL stream definition)
	 * @throws BindException if provided values (as well as defaults) failed to validate
	 */
	public abstract ModuleOptions interpolate(Map<String, String> raw) throws BindException;

}
