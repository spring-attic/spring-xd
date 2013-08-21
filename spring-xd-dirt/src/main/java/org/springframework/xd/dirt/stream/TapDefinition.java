/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.xd.dirt.stream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.BaseDefinition;

/**
 * @author David Turanski
 * @author Gunnnar Hillert
 * 
 * @since 1.0
 * 
 */
public class TapDefinition extends BaseDefinition {

	private String streamName;

	/**
	 * @param name - the tap name
	 * @param streamName - the tapped stream
	 * @param definition - the tap definition
	 * 
	 */
	public TapDefinition(String name, String streamName, String definition) {
		super(name, definition);
		Assert.hasText(streamName, "streamName cannot be empty or null");
		this.streamName = streamName;
	}

	/**
	 * @param name - the tap name
	 * @param definition - the tap definition
	 * 
	 */
	public TapDefinition(String name, String definition) {
		this(name, getStreamName(name, definition), definition);
	}

	/**
	 * @param definition
	 * @return the stream name
	 */
	public static String getStreamName(String name, String definition) {
		Pattern pattern = Pattern.compile("^\\s*tap\\s*@{0,1}\\s*(\\w+).*");
		Matcher matcher = pattern.matcher(definition);
		if (matcher.matches()) {
			return matcher.group(1);
		}
		return null;
	}

	/**
	 * @return the streamName
	 */
	public String getStreamName() {
		return streamName;
	}

	@Override
	public String toString() {
		return "TapDefinition [name=" + getName()
				+ ", definition=" + getDefinition() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((streamName == null) ? 0 : streamName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TapDefinition other = (TapDefinition) obj;
		if (streamName == null) {
			if (other.streamName != null) {
				return false;
			}
		}
		else if (!streamName.equals(other.streamName)) {
			return false;
		}
		return true;
	}

}
