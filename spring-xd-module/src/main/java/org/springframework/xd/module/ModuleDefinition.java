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

import java.util.Properties;

import org.springframework.core.io.Resource;

/**
 * Defines a module.
 * @author Gary Russell
 *
 */
public class ModuleDefinition {

	private final Resource resource;

	private volatile Properties properties;

	public ModuleDefinition(Resource resource) {
		this.resource = resource;
	}

	public Resource getResource() {
		return resource;
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

}
