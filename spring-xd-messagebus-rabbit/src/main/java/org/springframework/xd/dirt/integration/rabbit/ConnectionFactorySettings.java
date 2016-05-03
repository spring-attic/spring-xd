/*
 * Copyright 2011-2016 the original author or authors.
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
 *
 */

package org.springframework.xd.dirt.integration.rabbit;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * Configures the connection factory used by the rabbit message bus.
 *
 * @author Eric Bottard
 * @author Gary Russell
 * @author David Turanski
 */
@Configuration
public class ConnectionFactorySettings {

	@Value("${spring.rabbitmq.useSSL:false}")
	private boolean useSSL;

	@Value("${spring.rabbitmq.sslProperties:}")
	private Resource sslPropertiesLocation;

	@Value("${spring.rabbitmq.ssl.keyStore:}")
	private String keyStore;

	@Value("${spring.rabbitmq.ssl.keyStorePassphrase:}")
	private String keyStorePassphrase;

	@Value("${spring.rabbitmq.ssl.trustStore:}")
	private String trustStore;

	@Value("${spring.rabbitmq.ssl.trustStorePassphrase:}")
	private String trustStorePassphrase;

	@Value("${spring.rabbitmq.channelCacheSize:100}")
	private Integer channelCacheSize;

	@Bean
	// TODO: Move to spring boot
	public ConnectionFactory rabbitConnectionFactory(RabbitProperties config,
			com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory) throws Exception {
		CachingConnectionFactory factory = new CachingConnectionFactory(rabbitConnectionFactory);
		factory.setAddresses(config.getAddresses());
		if (config.getHost() != null) {
			factory.setHost(config.getHost());
			factory.setPort(config.getPort());
		}
		if (config.getUsername() != null) {
			factory.setUsername(config.getUsername());
		}
		if (config.getPassword() != null) {
			factory.setPassword(config.getPassword());
		}
		if (config.getVirtualHost() != null) {
			factory.setVirtualHost(config.getVirtualHost());
		}
		if (this.channelCacheSize != null) {
			factory.setChannelCacheSize(this.channelCacheSize);
		}
		return factory;
	}

	// If no RabbitProperties bean is available, instantiate one, deferring to Spring Boot for populating it
	@Configuration
	@ConditionalOnMissingBean(RabbitProperties.class)
	@EnableConfigurationProperties(RabbitProperties.class)
	public static class RabbitPropertiesLoader {
	}

	@Bean
	public RabbitConnectionFactoryBean rabbitFactory() {
		RabbitConnectionFactoryBean rabbitConnectionFactoryBean = new RabbitConnectionFactoryBean();
		rabbitConnectionFactoryBean.setUseSSL(this.useSSL);
		rabbitConnectionFactoryBean.setSslPropertiesLocation(this.sslPropertiesLocation);
		rabbitConnectionFactoryBean.setKeyStore(StringUtils.hasText(this.keyStore) ?
				this.keyStore : null);
		rabbitConnectionFactoryBean.setKeyStorePassphrase(StringUtils.hasText(
				this.keyStorePassphrase) ? this.keyStorePassphrase : null);
		rabbitConnectionFactoryBean.setTrustStore(StringUtils.hasText(this.trustStore) ?
				this.trustStore : null);
		rabbitConnectionFactoryBean.setTrustStorePassphrase(StringUtils.hasText(
				this.trustStorePassphrase) ? this.trustStorePassphrase : null);
		return rabbitConnectionFactoryBean;
	}

}
