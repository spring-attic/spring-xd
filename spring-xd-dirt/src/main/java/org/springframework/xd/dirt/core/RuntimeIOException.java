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

package org.springframework.xd.dirt.core;

import java.io.IOException;

import org.springframework.xd.dirt.DirtException;

/**
 * Thrown when something goes wrong during some IO operation. Unchecked, as opposed to the
 * {@link IOException} it wraps.
 * 
 * @author Eric Bottard
 */
@SuppressWarnings("serial")
public class RuntimeIOException extends DirtException {

	public RuntimeIOException(String message, IOException cause) {
		super(message, cause);
	}

}
