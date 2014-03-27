/*
 * Copyright 2011-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.integration.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.util.StringUtils;
import org.springframework.xd.integration.fixtures.Sinks;
import org.springframework.xd.integration.fixtures.Sources;

/**
 * Extracts the host and port information for the XD Instances
 * 
 * @author Glenn Renfro
 */
@Configuration
@PropertySource("classpath:application.properties")
public class XdEnvironment {

	// Environment Keys
	public static final String XD_ADMIN_HOST = "xd_admin_host";

	public static final String XD_CONTAINERS = "xd_containers";

	public static final String XD_HTTP_PORT = "xd_http_port";

	public static final String XD_JMX_PORT = "xd_jmx_port";

	// Result Environment Variables
	public final static String RESULT_LOCATION = "/tmp/xd/output";

	public static final String HTTP_PREFIX = "http://";

	private static final int SERVER_TYPE_OFFSET = 0;

	private static final int HOST_OFFSET = 1;

	private static final int XD_PORT_OFFSET = 2;

	private static final int HTTP_PORT_OFFSET = 3;

	private static final int JMX_PORT_OFFSET = 4;


	// Each line in the artifact has a entry to identify it as admin, container or singlenode.
	// These entries represent the supported types.
	public static final String ADMIN_TOKEN = "adminNode";

	public static final String CONTAINER_TOKEN = "containerNode";

	public static final String SINGLENODE_TOKEN = "singleNode";

	// The artifacts file name
	private static final String ARTIFACT_NAME = "ec2servers.csv";

	@Value("${xd_admin_host:}")
	private transient String adminServerValue;

	private transient URL adminServer;

	@Value("${xd_containers:}")
	private transient String containerValues;

	private transient List<URL> containers;

	@Value("${xd_jmx_port}")
	private transient int jmxPort;

	@Value("${xd_container_log_dir}")
	private transient String containerLogLocation;

	@Value("${xd_base_dir}")
	private transient String baseXdDir;

	@Value("${xd_http_port}")
	private transient int httpPort;

	@Value("${xd_pvt_keyfile:}")
	private String privateKeyFileName;

	private transient String privateKey;

	@Value("${xd_run_on_ec2:true}")
	private transient boolean isOnEc2;

	// JDBC Attributes
	@Value("${jdbc_url}")
	private transient String jdbcUrl;

	@Value("${jdbc_username}")
	private transient String jdbcUsername;

	@Value("${jdbc_password}")
	private transient String jdbcPassword;

	@Value("${jdbc_database}")
	private transient String jdbcDatabase;

	@Value("${jdbc_driver}")
	private transient String jdbcDriver;

	// JMS Attributes
	@Value("${jms_host}")
	private transient String jmsHost;

	@Value("${jms_port}")
	private transient int jmsPort;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(XdEnvironment.class);

	private final Properties artifactProperties;

	private boolean jmxInitialized = false;

	private boolean httpInitialized = false;

	public XdEnvironment() throws Exception {
		artifactProperties = getPropertiesFromArtifact();
	}


	public URL getAdminServer() throws MalformedURLException {
		if (adminServer == null) {
			if (artifactProperties != null) {
				adminServerValue = artifactProperties.getProperty(XD_ADMIN_HOST);
			}
			adminServer = new URL(adminServerValue);
		}
		return adminServer;
	}

	public List<URL> getContainers() {
		if (containers == null) {
			if (artifactProperties != null) {
				containerValues = artifactProperties.getProperty(XD_CONTAINERS);
			}
			containers = getContainers(containerValues);
		}
		return containers;
	}

	public int getJMXPort() {
		// if CLI artifact is present use it instead of environment.
		if (!jmxInitialized && artifactProperties != null) {
			jmxPort = Integer.parseInt(artifactProperties.getProperty(XD_JMX_PORT));
		}
		jmxInitialized = true;
		return jmxPort;
	}

	public int getHttpPort() {
		// if CLI artifact is present use it instead of environment.
		if (!httpInitialized && artifactProperties != null) {
			httpPort = Integer.parseInt(artifactProperties.getProperty(XD_HTTP_PORT));
		}
		httpInitialized = true;
		return httpPort;
	}

	public String getPrivateKey() throws IOException {
		// Initialize private key if not already set.
		if (isOnEc2 && privateKey == null) {
			isFilePresent(privateKeyFileName);
			privateKey = getPrivateKey(privateKeyFileName);
		}
		return privateKey;
	}

	public boolean isOnEc2() {
		return isOnEc2;
	}

	public String getContainerLogLocation() {
		return containerLogLocation;
	}

	public String getBaseDir() {
		return baseXdDir;
	}

	/**
	 * retrieves the private key from a file, so we can execute commands on the container.
	 * 
	 * @param privateKeyFile The location of the private key file
	 * @return
	 * @throws IOException
	 */
	private String getPrivateKey(String privateKeyFile) throws IOException {
		String result = "";
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(privateKeyFile));
			while (br.ready()) {
				result += br.readLine() + "\n";
			}
		}
		finally {
			if (br != null) {
				try {
					br.close();
				}
				catch (Exception ex) {
					// ignore error.
				}
			}
		}
		return result;
	}


	/**
	 * Retrieves a list o containers from the properties file.
	 * 
	 * @param properties
	 * @return
	 */
	private List<URL> getContainers(String containerList) {
		final List<URL> containers = new ArrayList<URL>();
		final Set<String> containerHosts = StringUtils
				.commaDelimitedListToSet(containerList);
		final Iterator<String> iter = containerHosts.iterator();
		while (iter.hasNext()) {
			final String containerHost = iter.next();
			try {
				containers.add(new URL(containerHost));
			}
			catch (MalformedURLException ex) {
				LOGGER.error("Container Host IP is invalid ==>" + containerHost);
			}

		}
		return containers;
	}

	private Properties getPropertiesFromArtifact() {
		Properties props = null;
		BufferedReader reader = null;
		String containerHosts = null;
		try {
			final File file = new File(ARTIFACT_NAME);
			if (file.exists()) {
				props = new Properties();
				reader = new BufferedReader(new FileReader(ARTIFACT_NAME));
				while (reader.ready()) {
					final String line = reader.readLine();
					final String tokens[] = StringUtils
							.commaDelimitedListToStringArray(line);
					if (tokens.length < 4) {
						continue;// skip invalid lines
					}
					if (tokens[SERVER_TYPE_OFFSET].equals(ADMIN_TOKEN)) {
						props.setProperty(XD_ADMIN_HOST, HTTP_PREFIX
								+ tokens[HOST_OFFSET] + ":"
								+ tokens[XD_PORT_OFFSET]);
						props.setProperty(XD_HTTP_PORT,
								tokens[HTTP_PORT_OFFSET]);
						props.setProperty(XD_JMX_PORT, tokens[JMX_PORT_OFFSET]);

					}
					if (tokens[SERVER_TYPE_OFFSET].equals(CONTAINER_TOKEN)) {
						if (containerHosts == null) {
							containerHosts = HTTP_PREFIX
									+ tokens[HOST_OFFSET].trim() + ":"
									+ tokens[XD_PORT_OFFSET];
						}
						else {
							containerHosts = containerHosts + "," + HTTP_PREFIX
									+ tokens[HOST_OFFSET].trim() + ":"
									+ tokens[XD_PORT_OFFSET];
						}
					}
					if (tokens[SERVER_TYPE_OFFSET].equals(SINGLENODE_TOKEN)) {
						props.setProperty(XD_ADMIN_HOST, HTTP_PREFIX
								+ tokens[HOST_OFFSET] + ":"
								+ tokens[XD_PORT_OFFSET]);
						props.setProperty(XD_HTTP_PORT,
								tokens[HTTP_PORT_OFFSET]);
						props.setProperty(XD_JMX_PORT, tokens[JMX_PORT_OFFSET]);

						containerHosts = HTTP_PREFIX
								+ tokens[HOST_OFFSET].trim() + ":" + tokens[XD_PORT_OFFSET];
						props.put(XD_CONTAINERS, containerHosts);
					}
				}
			}
		}
		catch (IOException ioe) {
			// Ignore file open error. Default to System variables.
		}
		finally {
			try {
				if (reader != null) {
					reader.close();
				}
			}
			catch (IOException ioe) {
				// ignore
			}
		}
		if (containerHosts != null) {
			props.put(XD_CONTAINERS, containerHosts);
		}
		return props;
	}


	private void isFilePresent(String keyFile) {
		File file = new File(keyFile);
		if (!file.exists()) {
			throw new IllegalArgumentException("The XD Private Key File ==> " + keyFile + " does not exist.");
		}
	}


	public String getJdbcUrl() {
		return jdbcUrl;
	}

	public String getJdbcUsername() {
		return jdbcUsername;
	}


	public String getJdbcPassword() {
		return jdbcPassword;
	}

	public String getJdbcDriver() {
		return jdbcDriver;
	}

	public String getJdbcDatabase() {
		return jdbcDatabase;
	}

	public int getJmxPort() {
		return jmxPort;
	}

	@Bean
	public static PropertySourcesPlaceholderConfigurer placeHolderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public static XdEc2Validation validation() {
		return new XdEc2Validation();
	}

	@Bean
	Sinks sinks() {
		return new Sinks(this);
	}

	@Bean
	Sources sources() throws IOException {
		return new Sources(getAdminServer(), getContainers(), httpPort, jmsHost, jmsPort);
	}

	@Bean
	ConfigUtil configUtil() throws IOException {
		return new ConfigUtil(isOnEc2, this);
	}

}
