/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.module.core;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;

/**
 * Base support class for modules, wrapping {@link ModuleDescriptor} and {@link ModuleDeploymentProperties}.
 *
 * @author Mark Fisher
 * @author David Turanski
 * @author Gary Russell
 */
public abstract class AbstractModule implements Module {

	private final ModuleDescriptor descriptor;

	private final ModuleDeploymentProperties deploymentProperties;


	public AbstractModule(ModuleDescriptor descriptor, ModuleDeploymentProperties deploymentProperties) {
		Assert.notNull(descriptor, "descriptor must not be null");
		Assert.notNull(deploymentProperties, "deploymentProperties must not be null");
		this.descriptor = descriptor;
		this.deploymentProperties = deploymentProperties;
	}

	@Override
	public String getName() {
		return this.descriptor.getModuleName();
	}

	@Override
	public ModuleType getType() {
		return this.descriptor.getType();
	}

	@Override
	public ModuleDescriptor getDescriptor() {
		return descriptor;
	}

	@Override
	public ModuleDeploymentProperties getDeploymentProperties() {
		return this.deploymentProperties;
	}


	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " [name=" + this.getName() + ", type=" + this.getType() + ", group=" +
				this.descriptor.getGroup() + ", index=" + this.descriptor.getIndex() + " @" +
				ObjectUtils.getIdentityHexString(this) + "]";
	}

	@Override
	public boolean shouldBind() {
		return true;
	}
}
