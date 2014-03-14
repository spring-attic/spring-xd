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

import javax.sql.DataSource;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.container.ContainerMetadata;
import org.springframework.xd.dirt.util.ConfigLocations;
import org.springframework.xd.dirt.zookeeper.EmbeddedZooKeeper;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;

@EnableAutoConfiguration(exclude = ServerPropertiesAutoConfiguration.class)
@ImportResource("classpath:" + ConfigLocations.XD_INTERNAL_CONFIG_ROOT + "xd-global-beans.xml")
@EnableBatchProcessing
public class ParentConfiguration {

	private static final String MBEAN_EXPORTER_BEAN_NAME = "XDParentConfigMBeanExporter";

	@Bean
	public ContainerMetadata containerMetadata() {
		return new ContainerMetadata();
	}

	@Configuration
	@Profile("cloud")
	protected static class CloudFoundryConfiguration {

		@Bean
		public DataSource dataSource() {
			Cloud cloud = cloud();
			return cloud.getServiceConnector("mysql", DataSource.class, null);
		}

		@Bean
		@Profile("rabbit")
		public ConnectionFactory rabbitConnectionFactory() {
			Cloud cloud = cloud();
			return cloud.getServiceConnector("rabbit", ConnectionFactory.class, null);
		}

		@Bean
		protected Cloud cloud() {
			CloudFactory cloudFactory = new CloudFactory();
			Cloud cloud = cloudFactory.getCloud();
			return cloud;
		}
	}


	@Configuration
	@Profile(SingleNodeApplication.SINGLE_PROFILE)
	protected static class SingleNodeZooKeeperConnectionConfiguration {

		@Value("${zk.client.connect:}")
		private String zkClientConnect;

		@Autowired(required = false)
		private EmbeddedZooKeeper server;

		@Bean
		public ZooKeeperConnection zooKeeperConnection() {
			// the embedded server accepts client connections on a dynamically determined port
			if (server != null) {
				zkClientConnect = "localhost:" + server.getClientPort();
			}
			return new ZooKeeperConnection(zkClientConnect);
		}
	}


	@Configuration
	@Profile("!" + SingleNodeApplication.SINGLE_PROFILE)
	protected static class DistributedZooKeeperConnectionConfiguration {

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


	@ConditionalOnExpression("${XD_JMX_ENABLED:false}")
	@EnableMBeanExport(defaultDomain = "xd.parent")
	protected static class JmxConfiguration {

		@Bean(name = MBEAN_EXPORTER_BEAN_NAME)
		public IntegrationMBeanExporter integrationMBeanExporter() {
			IntegrationMBeanExporter exporter = new IntegrationMBeanExporter();
			exporter.setDefaultDomain("xd.parent");
			return exporter;
		}
	}

}
