/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.stream;

/**
 * Captures runtime information about a deployed {@link JobDefinition}.
 * 
 * @author Eric Bottard
 */
public class Job extends BaseInstance<JobDefinition> implements Comparable<Job> {

	/**
	 * Create a new Job instance out of the given {@link JobDefinition}.
	 */
	public Job(JobDefinition definition) {
		super(definition);
	}

	@Override
	public int compareTo(Job other) {
		int diff = this.getDefinition().getName().compareTo(other.getDefinition().getName());
		return (diff != 0) ? diff : this.getStartedAt().compareTo(other.getStartedAt());
	}
}
