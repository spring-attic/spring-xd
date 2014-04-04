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

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.health.VanillaHealthIndicator;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.xd.dirt.container.ContainerAttributes;
import org.springframework.xd.dirt.util.ConfigLocations;
import org.springframework.xd.dirt.util.RuntimeUtils;

/**
 * Beans defined and imported here are in the global parent context, hence available to the entire hierarchy, including
 * Admins, Containers, and Modules.
 *
 * @author David Turanski
 */
@EnableAutoConfiguration(exclude = ServerPropertiesAutoConfiguration.class)
@ImportResource("classpath:" + ConfigLocations.XD_INTERNAL_CONFIG_ROOT + "xd-global-beans.xml")
@EnableBatchProcessing
public class ParentConfiguration implements EnvironmentAware {

	private static final String CONTAINER_ATTRIBUTES_PREFIX = "xd.container.";

	private ConfigurableEnvironment environment;

	@Bean
	public ContainerAttributes containerAttributes() {
		ContainerAttributes containerAttributes = new ContainerAttributes();
		containerAttributes.setHost(RuntimeUtils.getHost()).setIp(RuntimeUtils.getIpAddress()).setPid(
				RuntimeUtils.getPid());
		setConfiguredContainerAttributes(containerAttributes);
		return containerAttributes;
	}

	/**
	 * @param containerAttributes
	 */
	private void setConfiguredContainerAttributes(ContainerAttributes containerAttributes) {
		Map<String, String> attributes = new HashMap<String, String>();
		for (PropertySource<?> propertySource : environment.getPropertySources()) {
			if (propertySource instanceof EnumerablePropertySource) {
				EnumerablePropertySource<?> ps = (EnumerablePropertySource<?>) propertySource;
				for (String key : ps.getPropertyNames()) {
					if (key.startsWith(CONTAINER_ATTRIBUTES_PREFIX)) {
						String attributeKey = key.replaceAll(CONTAINER_ATTRIBUTES_PREFIX, "");
						attributes.put(attributeKey, environment.getProperty(key));
					}
				}
			}
		}
		containerAttributes.putAll(attributes);
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

	@Bean
	@ConditionalOnExpression("${endpoints.health.enabled:true}")
	public HealthEndpoint<Object> healthEndpoint() {
		return new HealthEndpoint<Object>(new VanillaHealthIndicator());
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = (ConfigurableEnvironment) environment;
	}
}
