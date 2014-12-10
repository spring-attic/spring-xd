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

package org.springframework.xd.test;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;

import org.springframework.util.SocketUtils;

/**
 * Support class to have random configuration for tests.
 * 
 * @author Ilayaperumal Gopinathan
 */
public class RandomConfigurationSupport {

	private static final String ADMIN_SERVER_PORT = "PORT";

	private static final String XD_DATA_HOME = "xd.data.home";

	private static final String HSQLDB_HOST = "hsql.server.host";

	private static final String HSQLDB_PORT = "hsql.server.port";

	private static final String HSQLDB_DBNAME = "hsql.server.dbname";

	private static final String HSQLDB_DATABASE = "hsql.server.database";

	private static final String ZK_NAMESPACE_DIR = "zk.namespace";

	private static String tmpDir = FileUtils.getTempDirectory().toString();

	private static String batchJobsDirectory = tmpDir;

	private final long now;

	private final int adminPort;

	private static String zkNamespaceDir = tmpDir + "/xd";

	public RandomConfigurationSupport() {
		now = System.currentTimeMillis();
		adminPort = SocketUtils.findAvailableTcpPort();
		setupRandomAdminServerPort();
		setupRandomHSQLDBConfig();
		disableJmx();
		setZKNamespacePath();
	}

	private void setupRandomHSQLDBConfig(String host) {
		System.setProperty(HSQLDB_HOST, host);
		System.setProperty(HSQLDB_PORT, String.valueOf(SocketUtils.findAvailableTcpPort()));
		System.setProperty(XD_DATA_HOME, batchJobsDirectory);
		System.setProperty(HSQLDB_DBNAME, "dbname-" + now);
		System.setProperty(HSQLDB_DATABASE, "database-" + now);
	}

	private void setupRandomHSQLDBConfig() {
		setupRandomHSQLDBConfig("localhost");
	}

	private void setupRandomAdminServerPort() {
		System.setProperty(ADMIN_SERVER_PORT, String.valueOf(adminPort));
	}

	private void disableJmx() {
		System.setProperty("XD_JMX_ENABLED", "false");
	}

	private void setZKNamespacePath() {
		System.setProperty(ZK_NAMESPACE_DIR, zkNamespaceDir.substring(1));
	}

	public String getAdminServerPort() {
		return String.valueOf(adminPort);
	}

	@AfterClass
	public static void cleanup() {
		// By default the data directory is located inside ${xd.data.home}/jobs
		// Refer batch.xml
		try {
			FileUtils.deleteDirectory(new File(batchJobsDirectory + "/jobs"));
			FileUtils.deleteDirectory(new File(zkNamespaceDir));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		batchJobsDirectory = tmpDir;
	}
}
