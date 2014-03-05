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

package org.springframework.xd.dirt.container;

import org.springframework.xd.dirt.event.AbstractEvent;

/**
 * Base class for events related to XD Containers.
 * 
 * @author Mark Fisher
 */
@SuppressWarnings("serial")
public class AbstractContainerEvent extends AbstractEvent<ContainerMetadata> {

	public AbstractContainerEvent(ContainerMetadata containerMetadata) {
		super(containerMetadata);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"timestamp\":" + this.getTimestamp() + ",");
		sb.append("\"type\":\"" + getType() + "\"");
		sb.append(",");
		AbstractContainerEvent e = this;
		sb.append("\"id\":\"" + e.getSource().getId() + "\"");
		sb.append("}");
		return sb.toString();
	}


}
