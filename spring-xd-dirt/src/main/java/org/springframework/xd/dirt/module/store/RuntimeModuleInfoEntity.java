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

package org.springframework.xd.dirt.module.store;


/**
 * Represents runtime module model info.
 * 
 * @author Ilayaperumal Gopinathan
 */
public class RuntimeModuleInfoEntity {

	private final String id;

	private final String containerId;

	private final String group;

	private final String index;

	private final String properties;

	public RuntimeModuleInfoEntity(String containerId, String group, String index, String properties) {
		this.id = containerId + ":" + group + ":" + index;
		this.containerId = containerId;
		this.group = group;
		this.index = index;
		this.properties = properties;
	}

	public String getId() {
		return id;
	}

	public String getContainerId() {
		return containerId;
	}

	public String getGroup() {
		return group;
	}

	public String getIndex() {
		return index;
	}

	public String getProperties() {
		return properties;
	}

}
