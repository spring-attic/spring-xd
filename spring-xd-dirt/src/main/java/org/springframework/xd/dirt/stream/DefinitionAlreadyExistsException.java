/*
 * Copyright 2011-2013 the original author or authors.
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

package org.springframework.xd.dirt.stream;

/**
 * Exception which is raised when a resource definition with the same name already exists.
 *
 * @author Luke Taylor
 */
@SuppressWarnings("serial")
public class DefinitionAlreadyExistsException extends StreamException {

	private final String offendingName;

	/**
	 * Create a new exception.
	 *
	 * @param offendingName name of the definition that conflicts
	 * @param message Exception message. Can use {@link String#format(String, Object...)} syntax to include the
	 *        offendingName
	 */
	public DefinitionAlreadyExistsException(String offendingName, String message) {
		super(String.format(message, offendingName));
		this.offendingName = offendingName;
	}

	/**
	 * Return the definition name that was in conflict.
	 */
	public String getOffendingName() {
		return offendingName;
	}

}
