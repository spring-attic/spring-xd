/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.xd.dirt.server.admin.deployment.zk;

import org.apache.curator.framework.recipes.cache.PathChildrenCache;

/**
 * Event that is fired after a admin leader is elected.
 *
 * @author Ilayaperumal Gopinathan
 */
public class SupervisorElectedEvent {

	/**
	 * {@link org.apache.curator.framework.recipes.cache.PathChildrenCache} for module deployment requests path
	 */
	private final PathChildrenCache moduleDeploymentRequests;

	/**
	 * {@link org.apache.curator.framework.recipes.cache.PathChildrenCache} for stream deployment requests path
	 */
	private final PathChildrenCache streamDeployments;

	/**
	 * {@link org.apache.curator.framework.recipes.cache.PathChildrenCache} for job deployment requests path
	 */
	private final PathChildrenCache jobDeployments;

	/**
	 * Construct LeaderElected event.
	 *
	 * @param moduleDeploymentRequests module deployment requests path cache
	 * @param streamDeployments        stream deployment requests path cache
	 * @param jobDeployments           job deployment requests path cache
	 */
	public SupervisorElectedEvent(PathChildrenCache moduleDeploymentRequests, PathChildrenCache streamDeployments,
			PathChildrenCache jobDeployments) {
		this.moduleDeploymentRequests = moduleDeploymentRequests;
		this.streamDeployments = streamDeployments;
		this.jobDeployments = jobDeployments;
	}

	public PathChildrenCache getModuleDeploymentRequests() {
		return moduleDeploymentRequests;
	}

	public PathChildrenCache getStreamDeployments() {
		return streamDeployments;
	}

	public PathChildrenCache getJobDeployments() {
		return jobDeployments;
	}
}
