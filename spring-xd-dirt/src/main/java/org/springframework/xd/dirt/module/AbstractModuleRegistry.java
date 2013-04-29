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

package org.springframework.xd.dirt.module;

import org.springframework.core.io.Resource;
import org.springframework.xd.dirt.core.Module;

/**
 * @author Mark Fisher
 */
public abstract class AbstractModuleRegistry implements ModuleRegistry {

	@Override
	public Module lookup(String name, String type) {
		Resource resource = this.loadResource(name, type);
		SimpleModule module = new SimpleModule(name, type);
		module.addComponents(resource);
		// TODO: add properties from a property registry
		return module;
	}

	protected abstract Resource loadResource(String name, String type);

}
