/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.xd.dirt.rest;

import static org.mockito.Mockito.mock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.xd.dirt.module.memory.InMemoryModuleDependencyRepository;
import org.springframework.xd.dirt.stream.DeploymentMessageSender;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamRepository;
import org.springframework.xd.dirt.stream.memory.InMemoryStreamDefinitionRepository;
import org.springframework.xd.dirt.stream.memory.InMemoryStreamRepository;

/**
 * @author Gunnar Hillert
 * @author Glenn Renfro
 * @author Mark Fisher
 */
@Configuration
public class StreamsControllerIntegrationWithRepositoryTestsConfig extends Dependencies {

	@Override
	@Bean
	public StreamDefinitionRepository streamDefinitionRepository() {
		return new InMemoryStreamDefinitionRepository(new InMemoryModuleDependencyRepository());
	}

	@Override
	@Bean
	public StreamRepository streamRepository() {
		return new InMemoryStreamRepository();
	}

	@Override
	@Bean
	public DeploymentMessageSender deploymentMessageSender() {
		return mock(DeploymentMessageSender.class);
	}

}
