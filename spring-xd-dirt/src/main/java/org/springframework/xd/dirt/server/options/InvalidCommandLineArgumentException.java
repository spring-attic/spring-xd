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

package org.springframework.xd.dirt.server.options;

import org.springframework.xd.dirt.core.XDRuntimeException;


/**
 * Thrown when there is an invalid command line argument passed into the runtime
 * 
 * @author Mark Pollack
 */
@SuppressWarnings("serial")
public class InvalidCommandLineArgumentException extends XDRuntimeException {

	/**
	 * Create a new InvalidCommandLineArgumentException
	 * 
	 * @param message description of the command line argument that was invalid
	 * @param cause the root cause
	 */
	public InvalidCommandLineArgumentException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Create a new InvalidCommandLineArgumentException
	 * 
	 * @param message description of the command line argument that was invalid
	 */
	public InvalidCommandLineArgumentException(String message) {
		super(message);
	}


}
