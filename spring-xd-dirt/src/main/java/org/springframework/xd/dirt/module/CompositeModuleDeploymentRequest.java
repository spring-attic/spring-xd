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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author Mark Fisher
 */
public class CompositeModuleDeploymentRequest extends ModuleDeploymentRequest {

	private volatile List<ModuleDeploymentRequest> children;

	public CompositeModuleDeploymentRequest() {
		// no-arg constructor for serialization
	}

	public CompositeModuleDeploymentRequest(ModuleDeploymentRequest parent, List<ModuleDeploymentRequest> children) {
		Assert.notNull(parent, "parent must not be null");
		Assert.notEmpty(children, "children must not be empty");
		this.setGroup(parent.getGroup());
		this.setIndex(parent.getIndex());
		this.setLaunch(parent.isLaunch());
		this.setModule(parent.getModule());
		this.setRemove(parent.isRemove());
		this.setSinkChannelName(parent.getSinkChannelName());
		this.setSourceChannelName(parent.getSourceChannelName());
		this.setType(parent.getType());
		this.setChildren(children);
		Map<String, String> parameters = parent.getParameters();
		if (!CollectionUtils.isEmpty(parameters)) {
			for (Map.Entry<String, String> entry : parameters.entrySet()) {
				this.setParameter(entry.getKey(), entry.getValue());
			}
		}
	}

	@Override
	public boolean isComposite() {
		return true;
	}

	public List<ModuleDeploymentRequest> getChildren() {
		return children;
	}

	void setChildren(List<ModuleDeploymentRequest> children) {
		Collections.sort(children);
		this.children = children;
	}

}
