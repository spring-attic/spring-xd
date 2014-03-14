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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationListener;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.server.SharedServerContextConfiguration;
import org.springframework.xd.dirt.server.options.ContainerOptions;
import org.springframework.xd.dirt.server.options.HadoopDistro;

/**
 * Initializer that can print useful stuff about an XD context on startup.
 *
 * @author Dave Syer
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 */
public class XdConfigLoggingInitializer implements ApplicationListener<ContextRefreshedEvent>, EnvironmentAware {

	private static Log logger = LogFactory.getLog(XdConfigLoggingInitializer.class);

	protected Environment environment;

	private final boolean isContainer;

	private static final String HADOOP_DISTRO_OPTION = "${HADOOP_DISTRO}";

	private static final String ZK_CONNECT_OPTION = "${" + SharedServerContextConfiguration.ZK_CONNECT + "}";

	private static final String EMBEDDED_ZK_CONNECT_OPTION = "${"
			+ SharedServerContextConfiguration.EMBEDDED_ZK_CONNECT + "}";

	public XdConfigLoggingInitializer(boolean isContainer) {
		this.isContainer = isContainer;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		logger.info("XD Home: " + environment.resolvePlaceholders("${XD_HOME}"));
		if (isContainer) {
			logger.info("Transport: " + environment.resolvePlaceholders("${XD_TRANSPORT}"));
			logHadoopDistro();
		}
		logZkConnectString();
		logger.info("Analytics: " + environment.resolvePlaceholders("${XD_ANALYTICS}"));
	}

	private void logHadoopDistro() {
		String hadoopDistro = environment.resolvePlaceholders(HADOOP_DISTRO_OPTION);
		logger.info("Hadoop Distro: " + hadoopDistro);
		String hdpVersionFromCP = org.apache.hadoop.util.VersionInfo.getVersion();
		Map<HadoopDistro, String> hadoopVersionsMap = ContainerOptions.getHadoopDistroVersions();
		// Check if the hadoop version being used is different from the hadoop distro option
		if (!hdpVersionFromCP.contains(hadoopVersionsMap.get(HadoopDistro.valueOf(hadoopDistro)))) {
			for (HadoopDistro key : hadoopVersionsMap.keySet()) {
				if (hdpVersionFromCP.contains(hadoopVersionsMap.get(key))) {
					logger.warn(String.format(
							"Hadoop version detected from classpath: %s but you provided '--hadoopDistro %s'. Did you mean '--hadoopDistro %s'?",
							hdpVersionFromCP,
							hadoopDistro,
							key.toString()));
					// TODO: log at error and exit the application?
				}
			}
		}
		else {
			logger.info("Hadoop version detected from classpath: " + hdpVersionFromCP);
		}
	}

	private void logZkConnectString() {
		String zkConnectString = environment.resolvePlaceholders(ZK_CONNECT_OPTION);
		String embeddedZkConnectString = environment.resolvePlaceholders(EMBEDDED_ZK_CONNECT_OPTION);
		String connectString = (!StringUtils.hasText(zkConnectString) && StringUtils.hasText(embeddedZkConnectString)) ? embeddedZkConnectString
				: zkConnectString;
		logger.info("Zookeeper at: " + connectString);
	}
}
