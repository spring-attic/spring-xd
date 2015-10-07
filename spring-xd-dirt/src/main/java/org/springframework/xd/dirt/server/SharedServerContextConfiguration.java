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

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.util.ConfigLocations;
import org.springframework.xd.dirt.util.XdProfiles;
import org.springframework.xd.dirt.zookeeper.EmbeddedZooKeeper;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;

/**
 * Beans defined and imported here are shared by the XD Admin Server and Container Server.
 * @author David Turanski
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 * @author Gary Russell
 * @author Patrick Peralta
 */
@Configuration
@EnableIntegration
@Import({PropertyPlaceholderAutoConfiguration.class, MessageBusExtensionsConfiguration.class})
@ImportResource({
		ConfigLocations.XD_CONFIG_ROOT + "bus/${XD_TRANSPORT}-bus.xml",
		ConfigLocations.XD_CONFIG_ROOT + "internal/repositories.xml",
		ConfigLocations.XD_CONFIG_ROOT + "analytics/${XD_ANALYTICS}-analytics.xml"
})
public class SharedServerContextConfiguration {

	public static final String ZK_CONNECT = "zk.client.connect";

	public static final String EMBEDDED_ZK_CONNECT = "zk.embedded.client.connect";

	public static final String ZK_PROPERTIES_SOURCE = "zk-properties";

	@Configuration
	@Profile(XdProfiles.SINGLENODE_PROFILE)
	static class SingleNodeZooKeeperConfig extends ZookeeperConnectionConfig {

		@Bean
		@ConditionalOnExpression("'${zk.client.connect}'.isEmpty()")
		EmbeddedZooKeeper embeddedZooKeeper() {
			if (zkEmbeddedServerPort != null) {
				return new EmbeddedZooKeeper(zkEmbeddedServerPort);
			}
			else {
				return new EmbeddedZooKeeper();
			}
		}

		@Value("${zk.client.connect:}")
		private String zkClientConnect;

		@Value("${zk.embedded.server.port:}")
		private Integer zkEmbeddedServerPort;


		// This is autowired, but not required, since the EmbeddedZooKeeper instance is conditional.
		@Autowired(required = false)
		EmbeddedZooKeeper embeddedZooKeeper;

		@Bean
		public ZooKeeperConnection zooKeeperConnection() {
			boolean isEmbedded = (embeddedZooKeeper != null);
			// the embedded server accepts client connections on a dynamically determined port
			if (isEmbedded) {
				zkClientConnect = "localhost:" + embeddedZooKeeper.getClientPort();
			}
			return setupZookeeperPropertySource(zkClientConnect, isEmbedded);
		}
	}


	@Configuration
	@Profile("!" + XdProfiles.SINGLENODE_PROFILE)
	static class DistributedZooKeeperConfig extends ZookeeperConnectionConfig {

		@Bean
		public ZooKeeperConnection zooKeeperConnection() {
			return setupZookeeperPropertySource(zkClientConnect, false);
		}
	}

	protected static class ZookeeperConnectionConfig implements EnvironmentAware {

		@Value("${zk.client.connect:}")
		protected String zkClientConnect;

		@Value("${zk.namespace:}")
		protected String zkNamespace;

		@Value("${zk.client.sessionTimeout:" + ZooKeeperConnection.DEFAULT_SESSION_TIMEOUT + "}")
		protected int zkSessionTimeout;

		@Value("${zk.client.connectionTimeout:" + ZooKeeperConnection.DEFAULT_CONNECTION_TIMEOUT + "}")
		protected int zkConnectionTimeout;

		@Value("${zk.client.initialRetryWait:" + ZooKeeperConnection.DEFAULT_INITIAL_RETRY_WAIT + "}")
		protected int zkInitialRetryWait;

		@Value("${zk.client.retryMaxAttempts:" + ZooKeeperConnection.DEFAULT_MAX_RETRY_ATTEMPTS + "}")
		protected int zkRetryMaxAttempts;

		// TODO: Consider a way to not require this property
		/*
		 * This is a flag optionally passed as a system property indicating the intention to inject some custom
		 * configuration to the ZooKeeper client connection. It is used here to prevent auto start of the connection
		 * since the configuration should be applied before the connection is started.
		 */
		@Value("${zk.client.connection.configured:false}")
		private boolean zkConnectionConfigured;

		private ConfigurableEnvironment environment;

		private Properties zkProperties = new Properties();

		protected ZooKeeperConnection setupZookeeperPropertySource(String zkClientConnect, boolean isEmbedded) {
			if (!StringUtils.hasText(zkClientConnect)) {
				zkClientConnect = ZooKeeperConnection.DEFAULT_CLIENT_CONNECT_STRING;
			}
			if (!StringUtils.hasText(zkNamespace)) {
				zkNamespace = Paths.XD_NAMESPACE;
			}
			if (isEmbedded) {
				zkProperties.put(EMBEDDED_ZK_CONNECT, zkClientConnect);
			}
			else {
				zkProperties.put(ZK_CONNECT, zkClientConnect);
			}
			this.environment.getPropertySources().addFirst(
					new PropertiesPropertySource(ZK_PROPERTIES_SOURCE, zkProperties));
			ZooKeeperConnection zooKeeperConnection = new ZooKeeperConnection(zkClientConnect, zkNamespace,
					zkSessionTimeout, zkConnectionTimeout, zkInitialRetryWait, zkRetryMaxAttempts);

			zooKeeperConnection.setAutoStartup(!zkConnectionConfigured);

			return zooKeeperConnection;
		}

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = (ConfigurableEnvironment) environment;
		}

	}
}
