/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.plugins.job.support.listener;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.batch.core.StepExecution;

/**
 * @author Gunnar Hillert
 * @since 1.0
 */
public class ChunkContextInfo implements Serializable {

	private boolean complete;

	private StepExecution stepExecution;

	private final Map<String, Object> attributes = new LinkedHashMap<String, Object>(0);

	public boolean isComplete() {
		return complete;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	public StepExecution getStepExecution() {
		return stepExecution;
	}


	public void setStepExecution(StepExecution stepExecution) {
		this.stepExecution = stepExecution;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public String toString() {
		return "XdChunkContextInfo [complete=" + complete + ", stepExecution=" + stepExecution + ", attributes="
				+ attributes + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result + (complete ? 1231 : 1237);
		result = prime * result + ((stepExecution == null) ? 0 : stepExecution.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChunkContextInfo other = (ChunkContextInfo) obj;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		}
		else if (!attributes.equals(other.attributes))
			return false;
		if (complete != other.complete)
			return false;
		if (stepExecution == null) {
			if (other.stepExecution != null)
				return false;
		}
		else if (!stepExecution.equals(other.stepExecution))
			return false;
		return true;
	}
}
