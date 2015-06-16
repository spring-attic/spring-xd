/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.util;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationListener;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.cluster.ContainerAttributes;
import org.springframework.xd.dirt.server.SharedServerContextConfiguration;

/**
 * Initializer that can print useful stuff about an XD context on startup.
 *
 * @author Dave Syer
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 *
 */
public class XdConfigLoggingInitializer implements ApplicationListener<ContextRefreshedEvent>, EnvironmentAware {

	private static Logger logger = LoggerFactory.getLogger(XdConfigLoggingInitializer.class);

	protected ConfigurableEnvironment environment;

	private final boolean isContainer;

	private static final String HADOOP_DISTRO_OPTION = "${HADOOP_DISTRO}";

	private static final String ADMIN_PORT = "${server.port}";

	private static final String XD_CONFIG_LOCATION = "${xd.config.home}";

	private static final String XD_CONFIG_NAME = "${spring.config.name:servers}";

	private static final String XD_MODULE_CONFIG_LOCATION = "${xd.module.config.location:${xd.config.home}/modules/}";

	private static final String XD_MODULE_CONFIG_NAME = "${xd.module.config.name:modules}";

	private static final String XD_ZK_NAMESPACE = "${zk.namespace}";

	private ContainerAttributes containerAttributes;

	public XdConfigLoggingInitializer(boolean isContainer) {
		this.isContainer = isContainer;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = (ConfigurableEnvironment) environment;
	}

	/**
	 * If not set, this property will default to {@code null}.
	 *
	 * @param containerAttributes Must not be null
	 */
	public void setContainerAttributes(ContainerAttributes containerAttributes) {
		Assert.notNull(containerAttributes);
		this.containerAttributes = containerAttributes;
	}

	/**
	 * Logger config info upon admin or container server context refresh.
	 */
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		logger.info("XD Home: " + environment.resolvePlaceholders("${XD_HOME}"));
		logger.info("Transport: " + environment.resolvePlaceholders("${XD_TRANSPORT}"));
		logHadoopDistro();
		logConfigLocations();
		if (isContainer) {
			logContainerInfo();
		}
		else {
			logAdminUI();
		}
		logZkConnectString();
		logZkNamespace();
		logger.info("Analytics: " + environment.resolvePlaceholders("${XD_ANALYTICS}"));
		if ("true".equals(environment.getProperty("verbose"))) {
			logAllProperties();
		}
	}

	private void logHadoopDistro() {
		if (ClassUtils.isPresent("org.apache.hadoop.util.VersionInfo", this.getClass().getClassLoader())) {
			logger.info("Hadoop version detected from classpath " + org.apache.hadoop.util.VersionInfo.getVersion());
		}
		else {
			logger.info("Hadoop version not detected from classpath");
		}
	}

	private void logContainerInfo() {

		final String containerIp;
		final String containerHostname;

		if (this.containerAttributes != null) {
			containerIp = containerAttributes.getIp();
			containerHostname = containerAttributes.getHost();
		}
		else {
			containerIp = "N/A";
			containerHostname = "N/A";
		}

		logger.info("Container IP address: " + containerIp);
		logger.info("Container hostname:   " + containerHostname);
	}

	/**
	 * Logger server/module config locations and names.
	 */
	private void logConfigLocations() {
		logger.info("XD config location: " + environment.resolvePlaceholders(XD_CONFIG_LOCATION));
		logger.info("XD config names: " + environment.resolvePlaceholders(XD_CONFIG_NAME));
		logger.info("XD module config location: " + environment.resolvePlaceholders(XD_MODULE_CONFIG_LOCATION));
		logger.info("XD module config name: " + environment.resolvePlaceholders(XD_MODULE_CONFIG_NAME));
	}

	/**
	 * Logger admin web UI URL
	 */
	private void logAdminUI() {
		String httpPort = environment.resolvePlaceholders(ADMIN_PORT);
		Assert.notNull(httpPort, "Admin server port is not set.");
		logger.info("Admin web UI: "
				+ String.format("http://%s:%s/%s", RuntimeUtils.getHost(), httpPort,
						ConfigLocations.XD_ADMIN_UI_BASE_PATH));
	}

	private void logZkConnectString() {
		PropertySource<?> zkPropertySource = environment.getPropertySources().get(
				SharedServerContextConfiguration.ZK_PROPERTIES_SOURCE);
		if (zkPropertySource != null) {
			String zkConnectString = (String) zkPropertySource.getProperty(SharedServerContextConfiguration.ZK_CONNECT);
			String embeddedZkConnectString = (String) zkPropertySource.getProperty(SharedServerContextConfiguration.EMBEDDED_ZK_CONNECT);
			String connectString = (!StringUtils.hasText(zkConnectString) && StringUtils.hasText(embeddedZkConnectString)) ? embeddedZkConnectString
					: zkConnectString;
			logger.info("Zookeeper at: " + connectString);
		}
	}

	private void logZkNamespace() {
		logger.info("Zookeeper namespace: " + environment.resolvePlaceholders(XD_ZK_NAMESPACE));
	}

	private void logAllProperties() {
		Set<String> propertyNames = new TreeSet<String>();

		for (PropertySource<?> ps : this.environment.getPropertySources()) {
			if (ps instanceof EnumerablePropertySource) {
				EnumerablePropertySource<?> eps = (EnumerablePropertySource<?>) ps;
				propertyNames.addAll(Arrays.asList(eps.getPropertyNames()));
			}
		}

		StringBuffer sb = new StringBuffer("\n");

		for (String key : propertyNames) {
			sb.append(String.format("\t%s=%s\n", key,
					environment.resolvePlaceholders(environment.getProperty(key).toString())));
		}
		logger.info(sb.toString());
	}
}
