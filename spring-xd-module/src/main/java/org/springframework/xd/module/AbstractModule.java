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

import java.util.Collection;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author Mark Fisher
 * @author David Turanski
 */
public abstract class AbstractModule implements Module {

	private final String name;

	private final String type;

	private volatile String instanceId;

	public AbstractModule(String name, String type) {
		Assert.notNull(name, "name must not be null");
		Assert.notNull(type, "type must not be null");
		this.name = name;
		this.type = type;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getType() {
		return this.type;
	}

	@Override
	public String getInstanceId() {
		return this.instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	@Override
	public String toString() {
		return "Module [name=" + name + ", type=" + type + "]";
	}
	
	@Override
	public void addComponents(Collection<Resource> resources) {
		if (CollectionUtils.isEmpty(resources)) {
			return;
		}
		Resource[] array = new Resource[resources.size()];
		addComponents(resources.toArray(array));
	}
}
