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

package org.springframework.xd.dirt.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.xd.module.core.CompositeModule;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A request to deploy a composite module. Contains embedded children deployment requests.
 * 
 * @author Mark Fisher
 * @author Eric Bottard
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class CompositeModuleDeploymentRequest extends ModuleDeploymentRequest {

	private volatile List<ModuleDeploymentRequest> children = new ArrayList<ModuleDeploymentRequest>();

	@SuppressWarnings("unused")
	private CompositeModuleDeploymentRequest() {
		// no-arg constructor for serialization
	}

	public CompositeModuleDeploymentRequest(ModuleDeploymentRequest parent, List<ModuleDeploymentRequest> children) {
		Assert.notNull(parent, "parent must not be null");
		Assert.notEmpty(children, "children must not be empty");
		this.setGroup(parent.getGroup());
		this.setIndex(parent.getIndex());
		this.setModule(parent.getModule());
		this.setSinkChannelName(parent.getSinkChannelName());
		this.setSourceChannelName(parent.getSourceChannelName());
		this.setType(parent.getType());
		this.setChildren(children);

		Map<String, String> parameters = parent.getParameters();
		// Pretend that options were set on the composed module itself
		// (eases resolution wrt defaults later)
		for (ModuleDeploymentRequest child : children) {
			for (String key : child.getParameters().keySet()) {
				String prefix = child.getModule() + CompositeModule.OPTION_SEPARATOR;
				this.setParameter(prefix + key, child.getParameters().get(key));
			}
		}

		// This is to copy options from parent to this (which may override
		// what was set above)
		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			this.setParameter(entry.getKey(), entry.getValue());
		}

		for (ModuleDeploymentRequest child : children) {
			child.setGroup(parent.getGroup() + "." + child.getModule());
		}
	}

	public List<ModuleDeploymentRequest> getChildren() {
		return children;
	}

	void setChildren(List<ModuleDeploymentRequest> children) {
		Collections.sort(children);
		this.children = children;
	}

}
