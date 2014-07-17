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

package org.springframework.xd.rest.domain;

import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.hateoas.ResourceSupport;


/**
 * Represents Batch job info.
 *
 * @author Ilayaperumal Gopinathan
 */
@XmlRootElement
public class JobInfoResource extends ResourceSupport {

	private String name;

	private int executionCount;

	private boolean launchable;

	private boolean incrementable;

	private Long jobInstanceId;

	private boolean deployed;

	/**
	 * Default constructor for serialization frameworks.
	 */
	protected JobInfoResource() {
	}

	public JobInfoResource(String name, int executionCount, Long jobInstanceId, boolean launchable,
			boolean incrementable, boolean deployed) {
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
	 * Return the deployment status from job info resource.
	 * @return deployed the boolean value to specify the deployment status
	 */
	public boolean isDeployed() {
		return deployed;
	}
}
