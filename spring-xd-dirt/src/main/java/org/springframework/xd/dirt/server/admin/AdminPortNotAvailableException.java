/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.server.admin;

import org.springframework.xd.dirt.DirtException;


/**
 * Exception thrown when the configured admin port is not available to use.
 *
 * @author Ilayaperumal Gopinathan
 */
@SuppressWarnings("serial")
public class AdminPortNotAvailableException extends DirtException {

	/**
	 * Construct an {@code AdminPortNotAvailableException}.
	 *
	 * @param adminPort the admin port being used
	 */
	public AdminPortNotAvailableException(String adminPort) {
		super("Admin port '" + adminPort + "' already in use.");
	}

}
