/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.xd.dirt.analytics;

import org.springframework.xd.dirt.DirtException;

/**
 * Thrown when trying to access a named metric that does not exist.
 *
 * @author Eric Bottard
 */
@SuppressWarnings("serial")
public class NoSuchMetricException extends DirtException {

	private final String offendingName;

	/**
	 * Construct a new exception. Message can contain {@link String#format(String, Object...)} placeholders and will be
	 * formatted with the offending name.
	 *
	 */
	public NoSuchMetricException(String offendingName, String message) {
		super(String.format(message, offendingName));
		this.offendingName = offendingName;
	}

	/**
	 * Return the name that was not found.
	 */
	public String getOffendingName() {
		return offendingName;
	}

}
