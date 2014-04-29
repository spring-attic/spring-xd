/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.integration.util;

import java.io.IOException;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.xd.integration.fixtures.Sinks;
import org.springframework.xd.integration.fixtures.Sources;


/**
 * 
 * @author mpollack
 */
@Configuration
@EnableAutoConfiguration
public class IntegrationTestConfig {

	@Bean
	public XdEnvironment xdEnvironment() {
		return new XdEnvironment();
	}

	@Bean
	public XdEc2Validation validation() {
		return new XdEc2Validation();
	}

	@Bean
	public Sinks sinks() {
		return new Sinks(xdEnvironment());
	}

	@Bean
	public Sources sources() {
		// The Environment Assumes that the RabbitMQ broker is running on the same host as the admin server.
		return new Sources(xdEnvironment());// getAdminServer().getHost(), getContainers().get(0).getHost(), jmsHost,
											// jmsPort);
	}

	@Bean
	public ConfigUtil configUtil() throws IOException {
		return new ConfigUtil(xdEnvironment());// isOnEc2, this);
	}
}
