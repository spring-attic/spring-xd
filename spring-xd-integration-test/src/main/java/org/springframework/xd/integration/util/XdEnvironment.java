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

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Extracts the host and port information for the XD Instances.
 *
 * Assumes that the host that runs the RabbitMQ broker is the same host that runs the admin server.
 *
 * @author Glenn Renfro
 */
public class XdEnvironment implements BeanClassLoaderAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(XdEnvironment.class);

	private volatile ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	// Environment Keys
	public static final String XD_ADMIN_HOST = "xd_admin_host";

	public static final String XD_CONTAINERS = "xd_containers";

	public static final String XD_HTTP_PORT = "xd_http_port";

	public static final String XD_JMX_PORT = "xd_jmx_port";

	// Result Environment Variables
	public static final String RESULT_LOCATION = "/tmp/xd/output";

	public static final String HTTP_PREFIX = "http://";


	@Value("${xd_admin_host:}")
	private String adminHost;

	private URL adminServerUrl;

	@Value("${xd_containers:}")
	private String containers;

	private List<URL> containerUrls;

	@Value("${xd_jmx_port}")
	private int jmxPort;

	@Value("${xd_container_log_dir}")
	private String containerLogLocation;

	@Value("${xd_base_dir}")
	private String baseXdDir;

	@Value("${xd_http_port}")
	private int httpPort;

	@Value("${xd_pvt_keyfile:}")
	private String privateKeyFileName;

	private String privateKey;

	@Value("${xd_run_on_ec2:true}")
	private boolean isOnEc2;

	// JDBC Attributes
	@Value("${jdbc_url}")
	private String jdbcUrl;

	@Value("${jdbc_username}")
	private String jdbcUsername;

	@Value("${jdbc_password}")
	private String jdbcPassword;

	@Value("${jdbc_database}")
	private String jdbcDatabase;

	@Value("${jdbc_driver}")
	private String jdbcDriver;

	// JMS Attributes
	@Value("${jms_host}")
	private String jmsHost;

	@Value("${jms_port}")
	private int jmsPort;

	private SimpleDriverDataSource dataSource;


	private Properties artifactProperties;

	/**
	 * If not running tests on a local XD Instance it will retrieve the information from the artifact and setup the
	 * environment to test against a remote XD. Als initializes the dataSource for JDBC Tests.
	 *
	 * @throws MalformedURLException
	 */
	@PostConstruct()
	public void initalizeEc2Environment() throws MalformedURLException {
		if (isOnEc2()) {
			artifactProperties = ConfigUtil.getPropertiesFromArtifact();
			adminServerUrl = new URL(artifactProperties.getProperty(XD_ADMIN_HOST));
			containerUrls = getContainerUrls(artifactProperties.getProperty(XD_CONTAINERS));
			jmxPort = Integer.parseInt(artifactProperties.getProperty(XD_JMX_PORT));
			httpPort = Integer.parseInt(artifactProperties.getProperty(XD_HTTP_PORT));
		}
		else {
			adminServerUrl = new URL(adminHost);
			containerUrls = getContainerUrls(containers);
		}

		if (jdbcUrl == null) {
			return;
		}
		dataSource = new SimpleDriverDataSource();
		try {
			@SuppressWarnings("unchecked")
			Class<? extends Driver> classz = (Class<? extends Driver>) ClassUtils.forName(jdbcDriver,
					beanClassLoader);
			dataSource.setDriverClass(classz);
		}
		catch (ClassNotFoundException e) {
			throw new IllegalStateException("failed to load class: " + jdbcDriver, e);
		}
		String resolvedJdbcUrl = String.format(jdbcUrl, jdbcDatabase);
		dataSource.setUrl(resolvedJdbcUrl);
		if (jdbcUsername != null) {
			dataSource.setUsername(jdbcUsername);
		}
		if (jdbcPassword != null) {
			dataSource.setPassword(jdbcPassword);
		}

	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	public URL getAdminServerUrl() {
		return adminServerUrl;
	}

	public List<URL> getContainerUrls() {
		return containerUrls;
	}

	public int getJmxPort() {
		return jmxPort;
	}

	public int getHttpPort() {
		return httpPort;
	}

	public String getPrivateKey() {
		// Initialize private key if not already set.
		if (isOnEc2 && privateKey == null) {
			privateKey = ConfigUtil.getPrivateKey(privateKeyFileName);
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

	public DataSource getDataSource() {
		return dataSource;
	}


	public String getJmsHost() {
		return jmsHost;
	}


	public int getJmsPort() {
		return jmsPort;
	}


	/**
	 * Default value is the same as the admin server host.
	 *
	 * @return the host where the rabbitmq broker is running.
	 */
	public String getRabbitMQHost() {
		return getAdminServerUrl().getHost();
	}

	/**
	 * The default target for http,tcp sources to send data to
	 *
	 * @return the first host in the collection of xd-container nodes.
	 */
	public String getDefaultTargetHost() {
		return getContainerUrls().get(0).getHost();
	}


	/**
	 * Retrieves a list o containers from the properties file.
	 *
	 * @param properties
	 * @return
	 */
	private static List<URL> getContainerUrls(String containerList) {
		final List<URL> containers = new ArrayList<URL>();
		final Set<String> containerHosts = StringUtils.commaDelimitedListToSet(containerList);
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


}
