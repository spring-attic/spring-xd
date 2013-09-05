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

package org.springframework.xd.dirt.plugins.job.batch;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hsqldb.Database;
import org.hsqldb.DatabaseManager;
import org.hsqldb.ServerConfiguration;
import org.hsqldb.persist.HsqlProperties;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;


/**
 * HSQL server mode
 * 
 * @author Thomas Risberg
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

		log.info("HSQL Server Startup sequence initiated");

		server.start();

		String portMsg = "port " + server.getPort();
		log.info("HSQL Server listening on " + portMsg);
	}

	@Override
	public void destroy() {
		// Do what it takes to shutdown
		log.info("HSQL Server Shutdown sequence initiated");
		server.signalCloseAllServerConnections();
		server.stop();
		DatabaseManager.closeDatabases(Database.CLOSEMODE_NORMAL);
		log.info("HSQL Server Shutdown completed");
		server = null;
	}

}
