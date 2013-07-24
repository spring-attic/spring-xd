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
 * @author Gary Russell
 */
public abstract class AbstractModule implements Module {

	private final String name;

	private final String type;

	private final String group;

	private final int index;

	private volatile String instanceId;

	public AbstractModule(String name, String type, String group, int index) {
		Assert.notNull(name, "name must not be null");
		Assert.notNull(type, "type must not be null");
		Assert.notNull(group, "group must not be null");
		this.name = name;
		this.type = type;
		this.group = group;
		this.index = index;
		this.instanceId = group + "." + index;
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
	public String getGroup() {
		return group;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getInstanceId() {
		return this.instanceId;
	}

	@Override
	public String toString() {
		return "AbstractModule [name=" + name + ", type=" + type + ", group=" + group + ", index=" + index + "]";
	}

}
