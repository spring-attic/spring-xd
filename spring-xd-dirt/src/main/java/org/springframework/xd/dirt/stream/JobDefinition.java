/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.xd.dirt.stream;

import org.springframework.xd.dirt.core.BaseDefinition;

/**
 * Represents a job in the system. Jobs are defined by a single module definition, referencing a Spring Batch Job as
 * compared to the more expressive DSL for defining streams. Job definitions are typically an expression of the form
 * {@code job --jobOption1=value1 --jobOption2=value2}.
 * 
 * @author David Turanski
 * @author Gunnar Hillert
 * 
 * @since 1.0
 * 
 */
public class JobDefinition extends BaseDefinition {

	@SuppressWarnings("unused")
	private JobDefinition() {
		// no-arg constructor for serialization
	}


	/**
	 * Create a new JobDefinition.
	 * 
	 * @param name the job name
	 * @param definition the job definition
	 * @param moduleDefinitions the module definitions derived by parsing the definition
	 */
	public JobDefinition(String name, String definition) {
		super(name, definition);
	}
}
