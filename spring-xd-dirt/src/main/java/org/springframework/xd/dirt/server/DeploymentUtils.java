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

package org.springframework.xd.dirt.server;


import org.apache.curator.framework.CuratorFramework;

import org.springframework.xd.dirt.core.Job;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.job.JobFactory;
import org.springframework.xd.dirt.stream.StreamFactory;

/**
 * Utility methods used for stream/job deployments.
 *
 * @author Ilayaperumal Gopinathan
 */
public abstract class DeploymentUtils {


	/**
	 * Utility for loading streams/jobs (including deployment metadata).
	 */
	private static final DeploymentLoader deploymentLoader = new DeploymentLoader();

	/**
	 * Load the {@link org.springframework.xd.dirt.core.Stream} instance
	 * for a given stream name <i>if the stream is deployed</i>. It will
	 * include the stream definition as well as any deployment properties
	 * data for the stream deployment.
	 *
	 * @param client         curator client
	 * @param streamName     the name of the stream to load
	 * @param streamFactory  stream factory used to create instance of stream
	 * @return the stream instance, or {@code null} if the stream does
	 *         not exist or is not deployed
	 * @throws Exception if ZooKeeper access fails for any reason
	 */
	public static Stream loadStream(CuratorFramework client, String streamName,
			StreamFactory streamFactory) throws Exception {
		return deploymentLoader.loadStream(client, streamName, streamFactory);
	}

	/**
	 * Load the {@link org.springframework.xd.dirt.core.Job}
	 * instance for a given job name<i>if the job is deployed</i>.
	 *
	 * @param client   curator client
	 * @param jobName  the name of the job to load
	 * @param jobFactory job factory used to create instance of job
	 * @return the job instance, or {@code null} if the job does not exist
	 *         or is not deployed
	 * @throws Exception
	 */
	public static Job loadJob(CuratorFramework client, String jobName,
			JobFactory jobFactory) throws Exception {
		return deploymentLoader.loadJob(client, jobName, jobFactory);
	}
}
