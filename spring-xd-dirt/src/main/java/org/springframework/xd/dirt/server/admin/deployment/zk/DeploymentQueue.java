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

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.queue.DistributedQueue;
import org.apache.curator.framework.recipes.queue.QueueBuilder;
import org.apache.curator.framework.recipes.queue.QueueConsumer;
import org.apache.curator.framework.recipes.queue.QueueSerializer;
import org.apache.curator.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentMessage;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * This class controls the lifecycle operations of the ZK distributed queue that
 * holds the {@link org.springframework.xd.dirt.server.admin.deployment.DeploymentMessage}s.
 *
 * @author Ilayaperumal Gopinathan
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class DeploymentQueue implements InitializingBean, DisposableBean {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(DeploymentQueue.class);

	/**
	 * ZK distributed queue for holding the deployment messages
	 */
	private DistributedQueue<DeploymentMessage> distributedQueue;

	/**
	 * Consumer that consumes the deployment messages out of ZK distributed queue
	 */
	private final QueueConsumer<DeploymentMessage> queueConsumer;

	/**
	 * Curator framework client
	 */
	private final CuratorFramework client;

	/**
	 * ZK path for ZK distributed queue
	 */
	private final String deploymentQueuePath;

	/**
	 * Object writer for serialization of deployment message
	 */
	private final ObjectWriter objectWriter = new ObjectMapper().writerWithType(DeploymentMessage.class);

	/**
	 * Object reader for de-serialization of deployment message
	 */
	private final ObjectReader objectReader = new ObjectMapper().reader(DeploymentMessage.class);

	/**
	 * Executor service to be used by the ZK distributed queue
	 */
	private final ExecutorService executorService;

	/**
	 * Construct deployment queue
	 * @param zkConnection the ZooKeeper connection
	 */
	public DeploymentQueue(ZooKeeperConnection zkConnection) {
		this(zkConnection.getClient(), null, Paths.DEPLOYMENT_QUEUE,
				Executors.newSingleThreadScheduledExecutor(ThreadUtils.newThreadFactory("DeploymentQueue")));
	}

	/**
	 * Construct deployment queue
	 *
	 * @param client the Curator framework client
	 * @param queueConsumer the consumer that consumes the deployment messages
	 * @param deploymentQueuePath the ZK path for the deployment queue
	 */
	public DeploymentQueue(CuratorFramework client, QueueConsumer queueConsumer, String deploymentQueuePath,
			ExecutorService executorService) {
		this.client = client;
		this.queueConsumer = queueConsumer;
		this.deploymentQueuePath = deploymentQueuePath;
		this.executorService = executorService;
	}

	/**
	 * Build and Start the ZK distributed queue.
	 * @throws Exception
	 */
	public void start() throws Exception {
		if (client != null) {
			QueueBuilder<DeploymentMessage> builder = QueueBuilder.builder(client, queueConsumer,
					new DeploymentMessageSerializer(), deploymentQueuePath);
			this.distributedQueue = builder.executor(executorService).buildQueue();
			this.distributedQueue.start();
		}
	}

	/**
	 * Get the underlying distributed queue.
	 * @return the distributed queue
	 */
	public DistributedQueue<DeploymentMessage> getDistributedQueue() {
		return distributedQueue;
	}

	/**
	 * Return the Curator client.
	 *
	 * @return the Curator client
	 */
	public CuratorFramework getClient() {
		return client;
	}

	@Override
	public void destroy() throws Exception {
		this.distributedQueue.close();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.start();
	}


	/**
	 * The queue serializer implementation to serialize/de-serialize {@link DeploymentMessage}
	 */
	private class DeploymentMessageSerializer implements QueueSerializer<DeploymentMessage> {

		/**
		 * De-serialize the byte[] to {@link DeploymentMessage}
		 *
		 * @param buffer byte[]
		 * @return the deployment message
		 */
		public DeploymentMessage deserialize(byte[] buffer) {
			DeploymentMessage deploymentMessage = null;
			try {
				deploymentMessage = objectReader.readValue(buffer);
			}
			catch (JsonProcessingException e) {
				logger.error("Json processing exception when de-serializing." + e);
			}
			catch (IOException ioe) {
				logger.error("IO exception exception when de-serializing." + ioe);
			}
			return deploymentMessage;
		}

		/**
		 * Serialize the deployment message
		 *
		 * @param message the deployment message
		 * @return byte array
		 */
		public byte[] serialize(DeploymentMessage message) {
			byte[] byteArray = null;
			try {
				byteArray = objectWriter.writeValueAsBytes(message);
			}
			catch (JsonMappingException e) {
				logger.error("Json processing exception when serializing." + e);
			}
			catch (IOException ioe) {
				logger.error("IO processing exception when de-serializing." + ioe);
			}
			return byteArray;
		}
	}
}
