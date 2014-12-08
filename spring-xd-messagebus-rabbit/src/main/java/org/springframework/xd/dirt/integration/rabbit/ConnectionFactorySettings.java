/*
 *
 *  * Copyright 2011-2014 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.xd.dirt.integration.rabbit;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;

/**
 * Configures the connection factory used by the rabbit message bus.
 *
 * @author Eric Bottard
 */
@Configuration
public class ConnectionFactorySettings {
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
		return factory;
	}

	@Bean
	@ConditionalOnMissingBean(RabbitProperties.class)
	RabbitProperties rabbitProperties() {
		return new RabbitProperties();
	}

	@Bean
	public RabbitConnectionFactoryBean rabbitFactory() {
		return new RabbitConnectionFactoryBean();
	}
}
