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
 * Exception thrown when getting message rate for
 * deployed modules.
 *
 * @author Ilayaperumal Gopinathan
 */
@SuppressWarnings("serial")
public class ModuleMessageRateNotFoundException extends Exception {

	/**
	 * Construct a {@code ModuleMessageRateNotFoundException}.
	 */
	public ModuleMessageRateNotFoundException() {
	}

	/**
	 * Construct a {@code ModuleMessageRateNotFoundException} with a description.
	 *
	 * @param message description for exception
	 */
	public ModuleMessageRateNotFoundException(String message) {
		super(message);
	}
}
