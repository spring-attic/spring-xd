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

package org.springframework.xd.dirt.plugins.job;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.xd.dirt.DirtException;


/**
 * Exception thrown when a batch job already exists in the {@link JobRegistry}.
 * 
 * @author Mark Pollack
 */
@SuppressWarnings("serial")
public class BatchJobAlreadyExistsInRegistryException extends DirtException {

	/**
	 * Creates a new {@link BatchJobAlreadyExistsInRegistryException} with the given job name.
	 * 
	 * @param name the name of the {@link Job} which should be unique
	 */
	public BatchJobAlreadyExistsInRegistryException(String name) {
		super("Batch job with name " + name + " already exists in the JobRegistry.");
	}
}
