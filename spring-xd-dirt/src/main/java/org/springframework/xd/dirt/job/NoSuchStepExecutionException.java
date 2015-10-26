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

package org.springframework.xd.dirt.job;

import org.springframework.batch.core.StepExecution;

/**
 * Thrown when attempting to refer to {@link StepExecution} that does not exist.
 * 
 * @author Ilayaperumal Gopinathan
 */
@SuppressWarnings("serial")
public class NoSuchStepExecutionException extends JobException {

	/**
	 * Create a new exception.
	 * 
	 * @param id the id of the {@link StepExecution} that could not be found
	 */
	public NoSuchStepExecutionException(long id) {
		super("Could not find step execution with id " + id);
	}

}
