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


/**
 * Represents Batch job info.
 * 
 * @author Dave Syer
 * @author Ilayaperumal Gopinathan
 * @since 1.0
 */
public class JobInfo {

	private final String name;

	private final int executionCount;

	private boolean launchable = false;

	private boolean incrementable = false;

	private final Long jobInstanceId;

	private boolean deployed = false;

	public JobInfo(String name, int executionCount, boolean launchable) {
		this(name, executionCount, null, launchable, false, false);
	}

	public JobInfo(String name, int executionCount, boolean launchable, boolean incrementable) {
		this(name, executionCount, null, launchable, incrementable, false);
	}

	public JobInfo(String name, int executionCount, boolean launchable, boolean incrementable, boolean deployed) {
		this(name, executionCount, null, launchable, incrementable, deployed);
	}

	public JobInfo(String name, int executionCount, Long jobInstanceId, boolean launchable, boolean incrementable,
			boolean deployed) {
		super();
		this.name = name;
		this.executionCount = executionCount;
		this.jobInstanceId = jobInstanceId;
		this.launchable = launchable;
		this.incrementable = incrementable;
		this.deployed = deployed;
	}

	public String getName() {
		return name;
	}

	public int getExecutionCount() {
		return executionCount;
	}

	public Long getJobInstanceId() {
		return jobInstanceId;
	}

	public boolean isLaunchable() {
		return launchable;
	}

	public boolean isIncrementable() {
		return incrementable;
	}

	public boolean isDeployed() {
		return deployed;
	}
}
