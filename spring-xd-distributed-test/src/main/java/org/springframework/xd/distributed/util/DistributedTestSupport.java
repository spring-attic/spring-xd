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

package org.springframework.xd.distributed.util;

import java.util.Map;
import java.util.Properties;

import com.oracle.tools.runtime.java.JavaApplication;
import com.oracle.tools.runtime.java.SimpleJavaApplication;

import org.springframework.xd.rest.client.impl.SpringXDTemplate;

/**
 * Provides support methods for building distributed system tests.
 * Implementations are responsible for starting up infrastructure
 * required for testing across multiple containers.
 *
 * @author Patrick Peralta
 */
public interface DistributedTestSupport {

	/**
	 * Start the minimum required servers for a distributed XD system:
	 * <ul>
	 *     <li>ZooKeeper</li>
	 *     <li>HSQL</li>
	 *     <li>Admin server (for serving REST endpoints)</li>
	 * </ul>
	 */
	void startup();

	/**
	 * Return an instance of {@link SpringXDTemplate} based on
	 * the port configured for the admin server started by {@link #startup}.
	 *
	 * @return REST template for executing commands against the admin server
	 */
	SpringXDTemplate ensureTemplate();

	/**
	 * Start a container in a new JVM, using the provided properties
	 * as system properties for the container JVM process.
	 *
	 * @param properties properties to provide to the container process
	 * @return JVM process for new container
	 */
	JavaApplication<SimpleJavaApplication> startContainer(Properties properties);

	/**
	 * Start a container in a new JVM.
	 *
	 * @return JVM process for new container
	 */
	JavaApplication<SimpleJavaApplication> startContainer();

	/**
	 * Block the executing thread until all of the container JVMs
	 * requested have started. This may also be used to ensure that a
	 * shut down container JVM process is no longer running.
	 *
	 * @return map of process ID to container UUId for currently
	 *         running container processes
	 * @throws InterruptedException
	 * @see org.springframework.xd.distributed.util.ServerProcessUtils#waitForContainers
	 */
	Map<Long, String> waitForContainers() throws InterruptedException;

	/**
	 * Shut down the container JVM with the given process id.
	 *
	 * @param pid process id of container JVM to shut down
	 */
	void shutdownContainer(long pid);

	/**
	 * Shut down all containers started via {@link #startContainer}.
	 * This method will block the executing thread until the
	 * admin server indicates that there are no containers running.
	 *
	 * @throws InterruptedException
	 */
	void shutdownContainers() throws InterruptedException;

	/**
	 * Shut down all servers started, including those started via
	 * {@link #startup} and all containers started via
	 * {@link #startContainer}.
	 *
	 * @throws java.lang.InterruptedException
	 */
	void shutdownAll() throws InterruptedException;
}

