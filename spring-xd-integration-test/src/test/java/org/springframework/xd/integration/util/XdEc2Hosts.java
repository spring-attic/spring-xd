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
import org.springframework.util.StringUtils;

/**
 * Extracts the host and port information for the XD Instances
 * @author Glenn Renfro
 */
public class XdEc2Hosts {

	public static final String XD_ADMIN_HOST = "xd-admin-host";
	public static final String XD_CONTAINERS = "xd-containers";
	public static final String XD_HTTP_PORT = "xd-http-port";	
	public static final String XD_JMX_PORT = "xd-jmx-port";

	
	public static final String HTTP_PREFIX = "http://";

	public static final String ADMIN_TOKEN = "adminNode";
	public static final String CONTAINER_TOKEN = "containerNode";

	private static final String ARTIFACT_NAME = "ec2servers.csv";

	private transient final URL adminServer;
	private transient final List<URL> containers;
	private transient final int jmxPort;
	private transient final int httpPort;
	
	private static final int SERVER_TYPE_OFFSET = 0;
	private static final int HOST_OFFSET = 1;
	private static final int XD_PORT_OFFSET = 2;
	private static final int HTTP_PORT_OFFSET =3;
	private static final int JMX_PORT_OFFSET = 4;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(XdEc2Hosts.class);

	public XdEc2Hosts() throws Exception{
		final Properties properties = getXDDeploymentProperties();
		containers = getContainers(properties);
		adminServer = getAdminServer(properties);
		jmxPort = Integer.parseInt(properties.getProperty(XD_JMX_PORT));
		httpPort = Integer.parseInt(properties.getProperty(XD_HTTP_PORT));
	}

	public URL getAdminServer() {
		return adminServer;
	}

	public List<URL> getContainers() {
		return containers;
	}

	public int getJMXPort() {
		return jmxPort;
	}

	public int getHttpPort() {
		return httpPort;
	}
	
	
	private URL getAdminServer(Properties properties) {

		URL result = null;
		final String host = properties.getProperty(XD_ADMIN_HOST);
		try {
			result = new URL(host);
		} catch (MalformedURLException mue) {
			LOGGER.info("XD_ADMIN_HOST was not identified in either an artifact or system environment variables");
		}
		return result;
	}

	private List<URL> getContainers(Properties properties) {
		final List<URL> containers = new ArrayList<URL>();
		final Set<String> containerHosts = StringUtils
				.commaDelimitedListToSet(properties.getProperty(XD_CONTAINERS));
		final Iterator<String> iter = containerHosts.iterator();
		while (iter.hasNext()) {
			final String containerHost = iter.next();
			try {
				containers.add(new URL(containerHost));
			} catch (MalformedURLException ex) {
				LOGGER.error("Container Host IP is invalid ==>" + containerHost);
			}

		}
		return containers;
	}


	private Properties getXDDeploymentProperties() {
		Properties result = getPropertiesFromArtifact();
		// if the artifact does not contain the admin try the System
		if (!result.containsKey(XD_ADMIN_HOST)
				|| !result.containsKey(XD_CONTAINERS)) {
			result = getPropertiesFromSystem();
		}
		if (!result.containsKey(XD_ADMIN_HOST)
				|| !result.containsKey(XD_CONTAINERS)) {
			throw new IllegalArgumentException(
					"No admin server host or XD_Containers has been set."
							+ "  This can be set via an artifact or setting the "
							+ XD_ADMIN_HOST + " and " + XD_CONTAINERS
							+ " environment variables");
		}

		return result;
	}

	private Properties getPropertiesFromArtifact() {
		final Properties props = new Properties();
		BufferedReader reader = null;
		String containerHosts = null;
		try {
			final File file = new File(ARTIFACT_NAME);
			if (file.exists()) {
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
								+ tokens[HOST_OFFSET]+":"+tokens[XD_PORT_OFFSET]);
						props.setProperty(XD_HTTP_PORT, tokens[HTTP_PORT_OFFSET]);
						props.setProperty(XD_JMX_PORT, tokens[JMX_PORT_OFFSET]);

					}
					if (tokens[SERVER_TYPE_OFFSET].equals(CONTAINER_TOKEN)) {
						if (containerHosts == null) {
							containerHosts = HTTP_PREFIX + tokens[HOST_OFFSET].trim()+":"+tokens[XD_PORT_OFFSET];
						} else {
							containerHosts = containerHosts + "," + HTTP_PREFIX
									+ tokens[HOST_OFFSET].trim()+":"+tokens[XD_PORT_OFFSET];
						}
					}
				}
			}
		} catch (IOException ioe) {
			// Ignore file open error. Default to System variables.
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException ioe) {
				// ignore
			}
		}
		if (containerHosts != null) {
			props.put(XD_CONTAINERS, containerHosts);
		}
		return props;
	}

	private Properties getPropertiesFromSystem() {
		final Properties props = new Properties();
		final Properties systemProperties = System.getProperties();
		if (systemProperties.containsKey(XD_ADMIN_HOST)) {
			props.put(XD_ADMIN_HOST,
					systemProperties.getProperty(XD_ADMIN_HOST));
		}
		if (systemProperties.containsKey(XD_CONTAINERS)) {
			props.put(XD_CONTAINERS,
					systemProperties.getProperty(XD_CONTAINERS));
		}
		if (systemProperties.containsKey(XD_JMX_PORT)) {
			props.put(XD_JMX_PORT,
					systemProperties.getProperty(XD_JMX_PORT));
		}
		if (systemProperties.containsKey(XD_HTTP_PORT)) {
			props.put(XD_HTTP_PORT,
					systemProperties.getProperty(XD_HTTP_PORT));
		}

		return props;
	}

}
