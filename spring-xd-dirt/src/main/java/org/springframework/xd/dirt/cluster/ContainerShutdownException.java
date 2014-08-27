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

package org.springframework.xd.dirt.cluster;


/**
 * Exception thrown when shutting down the container
 * from admin server.
 *
 * @author Ilayaperumal Gopinathan
 */
@SuppressWarnings("serial")
public class ContainerShutdownException extends Exception {

	/**
	 * Construct a {@code ContainerShutdownException}.
	 */
	public ContainerShutdownException() {
	}

	/**
	 * Construct a {@code ContainerShutdownException} with a description.
	 *
	 * @param message description for exception
	 */
	public ContainerShutdownException(String message) {
		super(message);
	}
}
