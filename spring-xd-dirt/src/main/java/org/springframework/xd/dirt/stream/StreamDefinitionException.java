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

package org.springframework.xd.dirt.stream;

/**
 * Thrown when a problem is detected with the (DSL) definition of a stream.
 * 
 * @author Eric Bottard
 */
@SuppressWarnings("serial")
public class StreamDefinitionException extends StreamDeploymentException {

	public StreamDefinitionException() {
		super();
	}

	public StreamDefinitionException(String message, Throwable cause) {
		super(message, cause);
	}

	public StreamDefinitionException(String message) {
		super(message);
	}

	public StreamDefinitionException(Throwable cause) {
		super(cause);
	}

}
