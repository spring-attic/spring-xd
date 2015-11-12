/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.batch.hsqldb.server;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.SocketException;
import java.util.Properties;

import com.sun.management.UnixOperatingSystemMXBean;
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.ServerConfiguration;
import org.hsqldb.server.ServerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;


/**
 * HSQL server mode
 * 
 * @author Thomas Risberg
 * @author Ilayaperumal Gopinathan
 */
public class HSQLServerBean implements InitializingBean, DisposableBean {

	/**
	 * Commons Logging instance.
	 */
	private static final Logger logger = LoggerFactory.getLogger(HSQLServerBean.class);

	/**
	 * Properties used to customize instance.
	 */
	private Properties serverProperties;

	/**
	 * The actual server instance.
	 */
	private org.hsqldb.Server server;

	public Properties getServerProperties() {
		return serverProperties;
	}

	public void setServerProperties(Properties serverProperties) {
		this.serverProperties = serverProperties;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		HsqlProperties configProps = serverProperties != null ? new HsqlProperties(serverProperties)
				: new HsqlProperties();

		ServerConfiguration.translateDefaultDatabaseProperty(configProps);

		// finished setting up properties - set some important behaviors as well;
		server = new org.hsqldb.Server();
		server.setLogWriter(null);
		server.setRestartOnShutdown(false);
		server.setNoSystemExit(true);
		server.setProperties(configProps);

		logger.debug("HSQL Database path: " + server.getDatabasePath(0, true));
		startServer();
	}

	private void startServer() throws Exception {
		logger.info("Starting HSQL Server database '" + server.getDatabaseName(0, true) + "' listening on port: "
				+ server.getPort());

		int tries = 0;
		boolean started;
		Throwable t = null;
		do {
			server.start();

			started = server.getState() == ServerConstants.SERVER_STATE_ONLINE;
			if (!started) {
				// The JavaDoc for server.start() claims to start the server synchronously;
				// however it only guarantees a transition to state SERVER_STATE_OPENING.
				// It is possible that the server is still starting up normally so
				// wait to see if it does.
				Thread.sleep(1000);
				started = server.getState() == ServerConstants.SERVER_STATE_ONLINE;
			}

			if (!started) {
				// On occasion the server will fail to start due to exception
				// "java.net.SocketException: Invalid argument"
				// when it calls socket.accept(). This exception mostly occurs
				// on Java 1.7 on OS X. This appears to be caused by having
				// > 1024 file descriptors open. Multiple attempts
				// will be made to start HSQLDB before giving up. An attempt
				// will be made every five seconds or sooner if the file
				// descriptor count drops below 1024.
				//
				// This Stack Overflow thread indicates that it happens on
				// Tomcat as well:
				//
				// http://stackoverflow.com/questions/16191236/
				// tomcat-startup-fails-due-to-java-net-socketexception-invalid-argument-on-mac-o
				//
				// This will be fixed in Java 7u60:
				//
				// https://bugs.openjdk.java.net/browse/JDK-8021820
				t = server.getServerError();
				if (t instanceof SocketException && "Invalid argument".equals(t.getMessage())) {
					long fileCount = getOpenFileDescriptorCount();

					logger.debug(
							String.format(
									"Caught SocketException (likely due to excessive file descriptors open; current count: %d)",
									fileCount), t);

					long timeout = System.currentTimeMillis() + 5000;
					while (System.currentTimeMillis() < timeout && fileCount > 1024) {
						Thread.sleep(500);
						fileCount = getOpenFileDescriptorCount();
					}

					logger.debug(String.format("Open files: %d", getOpenFileDescriptorCount()));
				}
				else {
					// if the server fails to start for any other reason,
					// break out of this loop instead of continuing to try
					// a restart
					break;
				}
			}
		}
		while (!started && ++tries < 5);

		if (started) {
			logger.info("Started HSQL Server");
		}
		else {
			String msg = String.format("HSQLDB could not be started on %s:%d, state: %s",
					server.getAddress(), server.getPort(), server.getStateDescriptor());

			if (t == null) {
				throw new IllegalStateException(msg);
			}
			else {
				throw new IllegalStateException(msg, t);
			}
		}

	}

	/**
	 * On UNIX operating systems, return the number of open file descriptors. On non UNIX operating systems this returns
	 * -1.
	 * 
	 * @return number of open file descriptors if this is executing on a UNIX operating system
	 */
	private long getOpenFileDescriptorCount() {
		OperatingSystemMXBean osStats = ManagementFactory.getOperatingSystemMXBean();
		return osStats instanceof UnixOperatingSystemMXBean
				? ((UnixOperatingSystemMXBean) osStats).getOpenFileDescriptorCount()
				: -1;
	}

	@Override
	public void destroy() {
		shutdownServer();
	}

	private void shutdownServer() {
		logger.info("HSQL Server Shutdown sequence initiated");
		if (server != null) {
			server.signalCloseAllServerConnections();
			server.stop();
			server.shutdown();
			// Wait until the server shuts down or timeout after 5 seconds.
			int attempts = 0;
			while (server.getState() != ServerConstants.SERVER_STATE_SHUTDOWN
					&& attempts++ < 50) {
				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
			if (server.getState() == ServerConstants.SERVER_STATE_SHUTDOWN) {
				logger.info("HSQL Server Shutdown completed.");
			}
			else {
				logger.warn("HSQL Server Shutdown timed out or was interrupted. Server State: " + server.getState());
			}
			server = null;
		}
	}
}
