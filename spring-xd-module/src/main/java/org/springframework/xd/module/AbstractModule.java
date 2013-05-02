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

import org.springframework.util.Assert;

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

}
