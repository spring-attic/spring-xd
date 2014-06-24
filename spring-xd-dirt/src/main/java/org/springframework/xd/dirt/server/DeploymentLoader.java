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

import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;

import org.springframework.xd.dirt.core.Job;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.job.JobFactory;
import org.springframework.xd.dirt.stream.StreamFactory;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;

/**
 * Utility for loading streams and jobs for the purpose of deployment.
 * <p/>
 * <b>Implementation note:</b> this should eventually be merged/replaced
 * with {@link org.springframework.xd.dirt.stream.zookeeper.ZooKeeperStreamRepository}
 * and {@link org.springframework.xd.dirt.stream.zookeeper.ZooKeeperJobRepository}.
 *
 * @see org.springframework.xd.dirt.server.ContainerListener
 * @see org.springframework.xd.dirt.server.JobDeploymentListener
 * @see org.springframework.xd.dirt.server.StreamDeploymentListener
 *
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 */
public class DeploymentLoader {

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
	public Job loadJob(CuratorFramework client, String jobName,
			JobFactory jobFactory) throws Exception {
		try {
			byte[] definition = client.getData().forPath(Paths.build(Paths.JOBS, jobName));
			Map<String, String> definitionMap = ZooKeeperUtils.bytesToMap(definition);

			byte[] deploymentPropertiesData = client.getData().forPath(
					Paths.build(Paths.JOB_DEPLOYMENTS, jobName));
			if (deploymentPropertiesData != null && deploymentPropertiesData.length > 0) {
				definitionMap.put("deploymentProperties", new String(deploymentPropertiesData, "UTF-8"));
			}
			return jobFactory.createJob(jobName, definitionMap);
		}
		catch (KeeperException.NoNodeException e) {
			// job is not deployed
		}
		return null;
	}

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
	public Stream loadStream(CuratorFramework client, String streamName,
			StreamFactory streamFactory) throws Exception {
		try {
			byte[] definition = client.getData().forPath(Paths.build(Paths.STREAMS, streamName));
			Map<String, String> definitionMap = ZooKeeperUtils.bytesToMap(definition);

			byte[] deploymentPropertiesData = client.getData().forPath(
					Paths.build(Paths.STREAM_DEPLOYMENTS, streamName));
			if (deploymentPropertiesData != null && deploymentPropertiesData.length > 0) {
				definitionMap.put("deploymentProperties", new String(deploymentPropertiesData, "UTF-8"));
			}
			return streamFactory.createStream(streamName, definitionMap);
		}
		catch (KeeperException.NoNodeException e) {
			// stream is not deployed or does not exist
		}
		return null;
	}

}
