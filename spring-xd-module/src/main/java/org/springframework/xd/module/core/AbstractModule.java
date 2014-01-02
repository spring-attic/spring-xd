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

package org.springframework.xd.module.core;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;

/**
 * Base support class for modules, wrapping {@link ModuleDefinition} and {@link DeploymentMetadata}.
 * 
 * @author Mark Fisher
 * @author David Turanski
 * @author Gary Russell
 */
public abstract class AbstractModule implements Module {

	private final ModuleDefinition definition;

	private final DeploymentMetadata metadata;

	public AbstractModule(ModuleDefinition definition, DeploymentMetadata metadata) {
		Assert.notNull(definition, "definition must not be null");
		Assert.notNull(metadata, "metadata must not be null");
		this.definition = definition;
		this.metadata = metadata;
	}

	@Override
	public String getName() {
		return this.definition.getName();
	}

	@Override
	public ModuleType getType() {
		return this.definition.getType();
	}

	protected ModuleDefinition getDefinition() {
		return definition;
	}

	@Override
	public DeploymentMetadata getDeploymentMetadata() {
		return this.metadata;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " [name=" + this.getName() + ", type=" + this.getType() + ", group="
				+ this.metadata.getGroup() + ", index=" + this.metadata.getIndex()
				+ " @" + ObjectUtils.getIdentityHexString(this) + "]";
	}

}
