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

package org.springframework.xd.dirt.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.util.ConfigLocations;
import org.springframework.xd.dirt.zookeeper.EmbeddedZooKeeper;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;


/**
 * Beans Shared by the XD Admin Server and Container Server
 * 
 * @author David Turanski
 */
@Configuration
@Import(PropertyPlaceholderAutoConfiguration.class)
@ImportResource({ ConfigLocations.XD_CONFIG_ROOT + "bus/*.xml",
	ConfigLocations.XD_CONFIG_ROOT + "store/${XD_STORE}-store.xml",
	ConfigLocations.XD_CONFIG_ROOT + "analytics/${XD_ANALYTICS}-analytics.xml" })
public class SharedServerContextConfiguration {

	@Value("${zk.client.connect:}")
	private String zkClientConnect;

	private static final String MBEAN_EXPORTER_BEAN_NAME = "XDSharedServerMBeanExporter";

	@ConditionalOnExpression("${XD_JMX_ENABLED:false}")
	@EnableMBeanExport(defaultDomain = "xd.shared.server")
	protected static class JmxConfiguration {

		@Bean(name = MBEAN_EXPORTER_BEAN_NAME)
		public IntegrationMBeanExporter integrationMBeanExporter() {
			IntegrationMBeanExporter exporter = new IntegrationMBeanExporter();
			exporter.setDefaultDomain("xd.shared.server");
			return exporter;
		}

	}


	@Configuration
	@Profile(SingleNodeApplication.SINGLE_PROFILE)
	static class SingleNodeZooKeeperConfig {

		@Bean
		@ConditionalOnExpression("'${zk.client.connect}'.isEmpty()")
		EmbeddedZooKeeper embeddedZooKeeper() {
			return new EmbeddedZooKeeper();
		}

		@Value("${zk.client.connect:}")
		private String zkClientConnect;

		// TODO: this seems weird that I have autowire this, but calling embeddedZooKeeper() below creates a different
		// instance than the bean version which starts the server thread and yields a different port
		@Autowired(required = false)
		EmbeddedZooKeeper zkServer;

		@Bean
		public ZooKeeperConnection zooKeeperConnection() {
			if (StringUtils.isEmpty(zkClientConnect)) {
				zkClientConnect = "localhost:" + zkServer.getClientPort();
			}
			return new ZooKeeperConnection(zkClientConnect);
		}
	}

	@Configuration
	@Profile("!" + SingleNodeApplication.SINGLE_PROFILE)
	static class DistributedZooKeeperConfig {

		@Value("${zk.client.connect:}")
		private String zkClientConnect;

		@Bean
		public ZooKeeperConnection zooKeeperConnection() {
			if (StringUtils.hasText(zkClientConnect)) {
				return new ZooKeeperConnection(zkClientConnect);
			}
			return new ZooKeeperConnection();
		}
	}


}
