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
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Luke Taylor
 * @author Ilayaperumal Gopinathan
 */
public class ModuleDeploymentRequest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private volatile String module;

	private volatile String group;

	private volatile String sourceChannelName;

	private volatile String sinkChannelName;

	private volatile int index;

	private volatile String type = "generic";

	private final Map<String, String> parameters = new HashMap<String, String>();

	private volatile boolean remove;

	private volatile boolean launch;

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isRemove() {
		return remove;
	}

	public void setRemove(boolean remove) {
		this.remove = remove;
	}

	public void setParameter(String name, String value) {
		this.parameters.put(name, value);
	}

	public Map<String, String> getParameters() {
		return Collections.unmodifiableMap(this.parameters);
	}

	public String getSourceChannelName() {
		return sourceChannelName;
	}

	public void setSourceChannelName(String sourceChannelName) {
		this.sourceChannelName = sourceChannelName;
	}

	public String getSinkChannelName() {
		return sinkChannelName;
	}

	public void setSinkChannelName(String sinkChannelName) {
		this.sinkChannelName = sinkChannelName;
	}

	public boolean isLaunch() {
		return launch;
	}

	public void setLaunch(boolean launch) {
		this.launch = launch;
	}

	@Override
	public String toString() {
		try {
			return this.objectMapper.writeValueAsString(this);
		}
		catch (Exception e) {
			return super.toString();
		}
	}

}
