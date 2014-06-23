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

package org.springframework.xd.module.options.types;

/**
 * This module option type represents a password. As such it will allow other components
 * within Spring XD to identify an option as password, and thus the ability to
 * provide custom view options (E.g. a password input field instead of a simple text box).
 *
 * @author Gunnar Hillert
 * @since 1.0
 */
public class Password {

	private final String password;

	public Password(String password) {
		this.password = password;
	}

	public String getPassword() {
		return password;
	}

	@Override
	public String toString() {
		return password;
	}
}
