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

package org.springframework.xd.rest.domain;


/**
 * Used to represent the context in which the code completion facility is invoked.
 * 
 * @author Eric Bottard
 */
public enum CompletionKind {

	/**
	 * A full stream definition, which ought to start with a source (or channel) and end with a sink (or channel).
	 */
	stream,

	/**
	 * A composed module, which starts or ends on a processor.
	 */
	module,

	/**
	 * A job definition.
	 */
	job;


}
