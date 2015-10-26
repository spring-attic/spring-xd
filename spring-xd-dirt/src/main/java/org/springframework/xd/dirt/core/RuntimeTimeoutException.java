/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.core;

import java.util.concurrent.TimeoutException;

import org.springframework.xd.dirt.DirtException;

/**
 * Exception thrown when a blocking operation times out. This is a runtime version of
 * {@link TimeoutException} and can accept an instance of {@code TimeoutException} as a
 * root cause.
 *
 * @author Patrick Peralta
 */
@SuppressWarnings("serial")
public class RuntimeTimeoutException extends DirtException {

	/**
	 * Construct a {@code RuntimeTimeoutException}.
	 *
	 * @param message detailed message
	 */
	public RuntimeTimeoutException(String message) {
		super(message);
	}

	/**
	 * Construct a {@code RuntimeTimeoutException}.
	 *
	 * @param message detailed message
	 * @param cause root cause exception
	 */
	public RuntimeTimeoutException(String message, TimeoutException cause) {
		super(message, cause);
	}
}
