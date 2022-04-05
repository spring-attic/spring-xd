/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.stream;

/**
 * Thrown when an attempt is made to deploy a definition that is already deployed.
 *
 * @author Eric Bottard
 *
 */
@SuppressWarnings("serial")
public class AlreadyDeployedException extends StreamException {

	/**
	 * Create a new exception.
	 *
	 * @param offendingName name of the definition that conflicts
	 * @param message Exception message. Can use {@link String#format(String, Object...)} syntax to include the
	 *        offendingName
	 */
	public AlreadyDeployedException(String offendingName, String message) {
		super(String.format(message, offendingName));
		this.offendingName = offendingName;
	}

	/**
	 * Create a new exception.
	 *
	 * @param offendingName name of the definition that conflicts
	 * @param message Exception message. Can use {@link String#format(String, Object...)} syntax to include the
	 *        offendingName
	 * @param cause root exception cause
	 */
	public AlreadyDeployedException(String offendingName, String message, Throwable cause) {
		super(String.format(message, offendingName), cause);
		this.offendingName = offendingName;
	}

	private final String offendingName;

	/**
	 * Return the name of the already deployed definition.
	 */
	public String getOffendingName() {
		return offendingName;
	}

}
