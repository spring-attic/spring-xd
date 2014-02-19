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

package org.springframework.xd.dirt.stream.redis;

import java.net.URL;

import org.springframework.core.io.Resource;
import org.springframework.xd.module.options.ModuleOptionsMetadata;

import com.fasterxml.jackson.annotation.JsonIgnore;


/**
 * Mixin for Jackson that will ignore the Resource, Classpath, Composed and ModuleOptionMetadata properties of a
 * ModuleDefinition.
 * 
 * @see ModuleDefinition.
 * 
 * @author Mark Pollack
 */
public interface ModuleDefinitionMixin {

	@JsonIgnore
	Resource getResource();

	@JsonIgnore
	boolean isComposed();

	@JsonIgnore
	URL[] getClasspath();

	@JsonIgnore
	ModuleOptionsMetadata getModuleOptionsMetadata();

}
