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

package org.springframework.xd.shell.command;


/**
 * Abstraction for a mechanism used to get user interactive user input.
 * 
 * @author Eric Bottard
 */
public interface UserInput {

	/**
	 * Display a prompt text to the user and expect one of {@code options} in return.
	 */
	public String prompt(String prompt, String defaultValue, String... options);

}
