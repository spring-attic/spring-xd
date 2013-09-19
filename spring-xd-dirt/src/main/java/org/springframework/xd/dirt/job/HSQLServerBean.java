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

package org.springframework.xd.dirt.job;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.ServerConfiguration;
import org.hsqldb.server.ServerConstants;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;


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
	private static final Log log = LogFactory.getLog(HSQLServerBean.class);

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

		log.debug("HSQL Database path: " + server.getDatabasePath(0, true));
		log.info("Starting HSQL Server database '" + server.getDatabaseName(0, true) + "' listening on port: "
				+ server.getPort());
		server.start();
		// server.start() is synchronous; so we should expect online status from server.
		Assert.isTrue(server.getState() == ServerConstants.SERVER_STATE_ONLINE,
				"HSQLDB not started yet.");
		log.info("Started HSQL Server");
	}

	@Override
	public void destroy() {
		log.info("HSQL Server Shutdown sequence initiated");
		if (server != null) {
			server.signalCloseAllServerConnections();
			server.stop();
			server.shutdown();
			// Wait until the server shuts down or break after 5 seconds.
			long start = System.currentTimeMillis();
			long end = start + 5 * 1000;
			while (server != null && server.getState() != ServerConstants.SERVER_STATE_SHUTDOWN) {
				try {
					if (System.currentTimeMillis() > end) {
						break;
					}
					Thread.sleep(100);
				}
				catch (InterruptedException e) {
				}
			}
			log.info("HSQL Server Shutdown completed");
			server = null;
		}
	}
}
