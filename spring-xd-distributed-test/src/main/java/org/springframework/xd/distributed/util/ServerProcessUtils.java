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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.curator.test.TestingServer;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.hateoas.PagedResources;
import org.springframework.util.Assert;
import org.springframework.xd.batch.hsqldb.server.HsqlServerApplication;
import org.springframework.xd.dirt.server.admin.AdminServerApplication;
import org.springframework.xd.dirt.server.container.ContainerServerApplication;
import org.springframework.xd.rest.client.impl.SpringXDTemplate;
import org.springframework.xd.rest.domain.DetailedContainerResource;

import com.oracle.tools.runtime.PropertiesBuilder;
import com.oracle.tools.runtime.console.SystemApplicationConsole;
import com.oracle.tools.runtime.java.JavaApplication;
import com.oracle.tools.runtime.java.NativeJavaApplicationBuilder;
import com.oracle.tools.runtime.java.SimpleJavaApplication;
import com.oracle.tools.runtime.java.SimpleJavaApplicationSchema;

/**
 * Collection of utilities for starting external processes required
 * for a distributed XD system.
 *
 * @author Patrick Peralta
 */
public class ServerProcessUtils {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ServerProcessUtils.class);


	/**
	 * Start an instance of ZooKeeper. Upon test completion, the method
	 * {@link org.apache.curator.test.TestingServer#stop()} should be invoked
	 * to shut down the server.
	 *
	 * @param port ZooKeeper port
	 * @return ZooKeeper testing server
	 * @throws Exception
	 */
	public static TestingServer startZooKeeper(int port) throws Exception {
		return new TestingServer(port);
	}

	/**
	 * Start an instance of the admin server. This method will block until
	 * the admin server is capable of processing HTTP requests. Upon test
	 * completion, the method {@link com.oracle.tools.runtime.java.JavaApplication#close()}
	 * should be invoked to shut down the server.
	 *
	 * @param properties system properties to pass to the container; at minimum
	 *                   must contain key {@code zk.client.connect} to indicate
	 *                   the ZooKeeper connect string and key {@code server.port}
	 *                   to indicate the admin server port
	 *
	 * @return admin server application reference
	 * @throws IOException            if an exception is thrown launching the process
	 * @throws InterruptedException   if the executing thread is interrupted
	 */
	public static JavaApplication<SimpleJavaApplication> startAdmin(Properties properties)
			throws IOException, InterruptedException {
		Assert.state(properties.containsKey("zk.client.connect"),
				"Property 'zk.client.connect' required");
		Assert.state(properties.containsKey("server.port"),
				"Property 'server.port' required");

		JavaApplication<SimpleJavaApplication> adminServer =
				launch(AdminServerApplication.class, false, properties, null);
		logger.debug("waiting for admin server");
		waitForAdminServer("http://localhost:" + properties.getProperty("server.port"));
		logger.debug("admin server ready");

		return adminServer;
	}

	/**
	 * Start a container instance.  Upon test completion, the method
	 * {@link com.oracle.tools.runtime.java.JavaApplication#close()}
	 * should be invoked to shut down the server. This method may also be
	 * invoked as part of failover testing.
	 * <p />
	 * Note that this method returns immediately. In order to verify
	 * that the container was started, invoke {@link #waitForContainers}
	 * to block until the container(s) are started.
	 *
	 * @param properties system properties to pass to the admin server; at minimum
	 *                   must contain key {@code zk.client.connect} to indicate
	 *                   the ZooKeeper connect string
	 *
	 * @return container server application reference
	 * @throws IOException if an exception is thrown launching the process
	 *
	 * @see #waitForContainers
	 */
	public static JavaApplication<SimpleJavaApplication> startContainer(Properties properties)
			throws IOException {
		Assert.state(properties.containsKey("zk.client.connect"),
				"Property 'zk.client.connect' required");

		return launch(ContainerServerApplication.class, false, properties, null);
	}

	/**
	 * Start an instance of HSQL. Upon test completion, the method
	 * {@link com.oracle.tools.runtime.java.JavaApplication#close()}
	 * should be invoked to shut down the server.
	 *
	 * @param systemProperties system properties for new process
	 * @return HSQL server application reference
	 * @throws IOException if an exception is thrown launching the process
	 */
	public static JavaApplication<SimpleJavaApplication> startHsql(Properties systemProperties)
			throws IOException {
		return launch(HsqlServerApplication.class, false, systemProperties, null);
	}

	/**
	 * Block the executing thread until all of the indicated process IDs
	 * have been identified in the list of runtime containers as indicated
	 * by the admin server. If an empty list is provided, the executing
	 * thread will block until there are no containers running.
	 *
	 * @param template REST template used to communicate with the admin server
	 * @param pids     set of process IDs for the expected containers
	 * @return map of process id to container id
	 * @throws InterruptedException            if the executing thread is interrupted
	 * @throws java.lang.IllegalStateException if the number of containers identified
	 *         does not match the number of PIDs provided
	 */
	public static Map<Long, String> waitForContainers(SpringXDTemplate template,
			Set<Long> pids) throws InterruptedException, IllegalStateException {
		int pidCount = pids.size();
		Map<Long, String> mapPidUuid = getRunningContainers(template);
		long expiry = System.currentTimeMillis() + 3 * 60000;

		while (mapPidUuid.size() != pidCount && System.currentTimeMillis() < expiry) {
			Thread.sleep(500);
			mapPidUuid = getRunningContainers(template);
		}

		if (mapPidUuid.size() == pidCount && mapPidUuid.keySet().containsAll(pids)) {
			return mapPidUuid;
		}

		Set<Long> missingPids = new HashSet<Long>(pids);
		missingPids.removeAll(mapPidUuid.keySet());

		Set<Long> unexpectedPids = new HashSet<Long>(mapPidUuid.keySet());
		unexpectedPids.removeAll(pids);

		StringBuilder builder = new StringBuilder();
		if (!missingPids.isEmpty()) {
			builder.append("Admin server did not find the following container PIDs:")
					.append(missingPids);
		}
		if (!unexpectedPids.isEmpty()) {
			if (builder.length() > 0) {
				builder.append("; ");
			}
			builder.append("Admin server found the following unexpected container PIDs:")
					.append(unexpectedPids);
		}

		throw new IllegalStateException(builder.toString());
	}

	/**
	 * Return a map of running containers. Map key is pid; value is
	 * the container ID.
	 *
	 * @param template REST template used to communicate with the admin server
	 * @return map of process id to container id
	 */
	public static Map<Long, String> getRunningContainers(SpringXDTemplate template) {
		Map<Long, String> mapPidUuid = new HashMap<Long, String>();
		PagedResources<DetailedContainerResource> containers =
				template.runtimeOperations().listContainers();
		for (DetailedContainerResource container : containers) {
			logger.trace("Container: {}", container);
			long pid = Long.parseLong(container.getAttribute("pid"));
			mapPidUuid.put(pid, container.getAttribute("id"));
		}

		return mapPidUuid;
	}

	/**
	 * Block the executing thread until the admin server is responding to
	 * HTTP requests.
	 *
	 * @param url URL for admin server
	 * @throws InterruptedException            if the executing thread is interrupted
	 * @throws java.lang.IllegalStateException if a successful connection to the
	 *         admin server was not established
	 */
	private static void waitForAdminServer(String url) throws InterruptedException, IllegalStateException {
		boolean connected = false;
		Exception exception = null;
		int httpStatus = 0;
		long expiry = System.currentTimeMillis() + 30000;
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();

		try {
			do {
				try {
					Thread.sleep(100);

					HttpGet httpGet = new HttpGet(url);
					httpStatus = httpClient.execute(httpGet).getStatusLine().getStatusCode();
					if (httpStatus == HttpStatus.SC_OK) {
						connected = true;
					}
				}
				catch (IOException e) {
					exception = e;
				}
			}
			while ((!connected) && System.currentTimeMillis() < expiry);
		}
		finally {
			try {
				httpClient.close();
			}
			catch (IOException e) {
				// ignore exception on close
			}
		}

		if (!connected) {
			StringBuilder builder = new StringBuilder();
			builder.append("Failed to connect to '").append(url).append("'");
			if (httpStatus > 0) {
				builder.append("; last HTTP status: ").append(httpStatus);
			}
			if (exception != null) {
				StringWriter writer = new StringWriter();
				exception.printStackTrace(new PrintWriter(writer));
				builder.append("; exception: ")
						.append(exception.toString())
						.append(", ").append(writer.toString());
			}
			throw new IllegalStateException(builder.toString());
		}
	}

	/**
	 * Launch the given class's {@code main} method in a separate JVM.
	 *
	 * @param clz               class to launch
	 * @param remoteDebug       if true, enable remote debugging
	 * @param systemProperties  system properties for new process
	 * @param args              command line arguments
	 * @return launched application
	 *
	 * @throws IOException if an exception was thrown launching the process
	 */
	private static JavaApplication<SimpleJavaApplication> launch(Class<?> clz,
			boolean remoteDebug, Properties systemProperties, List<String> args) throws IOException {
		String classpath = System.getProperty("java.class.path");

		logger.info("Launching {}", clz);
		logger.info("	args: {}", args);
		logger.info("	properties: {}", systemProperties);
		logger.info("	classpath: {}", classpath);

		SimpleJavaApplicationSchema schema = new SimpleJavaApplicationSchema(clz.getName(), classpath);
		if (args != null) {
			for (String arg : args) {
				schema.addArgument(arg);
			}
		}
		if (systemProperties != null) {
			schema.setSystemProperties(new PropertiesBuilder(systemProperties));
		}

		NativeJavaApplicationBuilder<SimpleJavaApplication, SimpleJavaApplicationSchema> builder =
				new NativeJavaApplicationBuilder<SimpleJavaApplication, SimpleJavaApplicationSchema>();
		builder.setRemoteDebuggingEnabled(remoteDebug);
		return builder.realize(schema, clz.getName(), new SystemApplicationConsole());
	}

}
