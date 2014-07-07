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

package org.springframework.xd.integration.test;

import java.io.IOException;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.xd.integration.fixtures.Jobs;
import org.springframework.xd.integration.fixtures.Processors;
import org.springframework.xd.integration.fixtures.Sinks;
import org.springframework.xd.integration.fixtures.Sources;
import org.springframework.xd.integration.util.ConfigUtil;
import org.springframework.xd.integration.util.HadoopUtils;
import org.springframework.xd.integration.util.XdEc2Validation;
import org.springframework.xd.integration.util.XdEnvironment;


/**
 * Provides the container configuration when running integration tests. Declares {@link XdEnvironment},
 * {@link XdEc2Validation}, {@link Sinks}, {@link Sources} and {@link ConfigUtil} in the application context.
 *
 * @author Mark Pollack
 */
@Configuration
@EnableAutoConfiguration
public class IntegrationTestConfig {

	@Bean
	public XdEnvironment xdEnvironment() {
		return new XdEnvironment();
	}

	@Bean
	public XdEc2Validation validation() throws IOException {
		return new XdEc2Validation(hadoopUtil(), xdEnvironment());
	}

	@Bean
	public Sinks sinks() {
		return new Sinks(xdEnvironment());
	}

	@Bean
	public Sources sources() {
		// The Environment Assumes that the RabbitMQ broker is running on the same host as the admin server.
		return new Sources(xdEnvironment());

	}

	@Bean
	public Jobs jobs() {
		return new Jobs(xdEnvironment());
	}

	@Bean
	public Processors processors() {
		return new Processors();
	}

	@Bean
	public ConfigUtil configUtil() throws IOException {
		return new ConfigUtil();
	}

	@Bean
	HadoopUtils hadoopUtil() throws IOException {
		return new HadoopUtils(xdEnvironment());
	}
}
