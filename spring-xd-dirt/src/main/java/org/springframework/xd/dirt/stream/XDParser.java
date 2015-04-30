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

package org.springframework.xd.dirt.stream;

import java.util.List;

import org.springframework.xd.module.ModuleDescriptor;

/**
 * Parser to convert a DSL string for a deployable unit (i.e. stream, job)
 * into a list of {@link org.springframework.xd.module.ModuleDescriptor}
 * objects that comprise the given unit.
 *
 * @author Mark Fisher
 * @author Patrick Peralta
 */
public interface XDParser {

	/**
	 * Parse a DSL string.
	 *
	 * @param name    name of the deployable unit, such as a stream or job
	 * @param config  the DSL string
	 * @param type    the context under which the parsing is occurring
	 *                (for example, is it a stream, module, or job being
	 *                parsed, how far into the DSL has parsing occurred, etc)
	 * @return list of {@link org.springframework.xd.module.ModuleDescriptor}
	 *         that reflect the modules required for the deployable unit
	 *         described by the DSL.
	 */
	List<ModuleDescriptor> parse(String name, String config, ParsingContext type);



}
