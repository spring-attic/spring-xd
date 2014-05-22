/*
 * Copyright 2013-2014 the original author or authors.
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

	/**
	 * Construct JobInfo
	 *
	 * @param name the name of the job
	 * @param executionCount the number of executions
	 * @param launchable flag to specify if the job is launchable
	 */
	public JobInfo(String name, int executionCount, boolean launchable) {
		this(name, executionCount, null, launchable, false, false);
	}

	/**
	 * Construct JobInfo
	 *
	 * @param name the name of the job
	 * @param executionCount the number of executions
	 * @param launchable flag to specify if the job is launchable
	 * @param incrementable flag to specify if the job parameter is incrementable
	 */
	public JobInfo(String name, int executionCount, boolean launchable, boolean incrementable) {
		this(name, executionCount, null, launchable, incrementable, false);
	}

	/**
	 * Construct JobInfo
	 *
	 * @param name the name of the job
	 * @param executionCount the number of executions
	 * @param launchable flag to specify if the job is launchable
	 * @param incrementable flag to specify if the job parameter is incrementable
	 */
	public JobInfo(String name, int executionCount, boolean launchable, boolean incrementable, boolean deployed) {
		this(name, executionCount, null, launchable, incrementable, deployed);
	}

	/**
	 * Construct JobInfo
	 *
	 * @param name the name of the job
	 * @param executionCount the number of executions
	 * @param jobInstanceId the job instnace id associated with this job info
	 * @param launchable flag to specify if the job is launchable
	 * @param incrementable flag to specify if the job parameter is incrementable
	 * @param deployed flag to specify if the job is deployed
	 */
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

	/**
	 * Return the deployment status from job info.
	 * @return deployed the boolean value to specify the deployment status
	 */
	public boolean isDeployed() {
		return deployed;
	}
}
