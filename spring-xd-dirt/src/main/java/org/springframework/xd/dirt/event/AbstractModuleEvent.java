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

package org.springframework.xd.dirt.event;

import org.springframework.xd.module.core.Module;

/**
 * @author Mark Fisher
 */
@SuppressWarnings("serial")
public abstract class AbstractModuleEvent extends AbstractEvent<Module> {

	private final String containerId;

	public AbstractModuleEvent(Module module, String containerId) {
		super(module);
		this.containerId = containerId;
	}

	public String getContainerId() {
		return this.containerId;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"timestamp\":" + this.getTimestamp() + ",");
		sb.append("\"type\":\"" + getType() + "\"");
		sb.append(",");
		AbstractModuleEvent e = this;
		sb.append("\"container\":\"" + e.getContainerId() + "\",");
		Module m = e.getSource();
		sb.append("\"source\":{\"name\":\"" + m.getName() + "\",");
		sb.append("\"type\":\"" + m.getType() + "\",");
		sb.append("\"running\":" + m.isRunning());
		sb.append("}");
		sb.append("}");
		return sb.toString();
	}

}
