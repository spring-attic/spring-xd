/*
 * Copyright 2002-2013 the original author or authors.
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
import org.springframework.xd.dirt.stream.DeploymentMessageSender;
import org.springframework.xd.dirt.stream.EnhancedStreamParser;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.XDParser;
import org.springframework.xd.dirt.stream.TapDefinitionRepository;
import org.springframework.xd.dirt.stream.TapDeployer;
import org.springframework.xd.dirt.stream.TapInstanceRepository;
import org.springframework.xd.dirt.stream.memory.InMemoryStreamDefinitionRepository;
import org.springframework.xd.dirt.stream.memory.InMemoryTapDefinitionRepository;
import org.springframework.xd.dirt.stream.memory.InMemoryTapInstanceRepository;

/**
 * @author David Turanski
 * @author Gunnar Hillert
 *
 * @since 1.0
 *
 */
@Configuration
public class TapsControllerIntegrationTestsConfig extends Dependencies {

	@Bean
	public StreamDefinitionRepository streamDefinitionRepository() {
		return new InMemoryStreamDefinitionRepository();
	}
	@Bean
	public TapDefinitionRepository tapDefinitionRepository() {
		return new InMemoryTapDefinitionRepository();
	}

	@Bean
	public TapInstanceRepository tapInstanceRepository() {
		return new InMemoryTapInstanceRepository();
	}

	@Bean
	public TapDeployer tapDeployer() {
		XDParser parser = new EnhancedStreamParser(
				tapDefinitionRepository(), moduleRegistry());
		return new TapDeployer(tapDefinitionRepository(),
				streamDefinitionRepository(), deploymentMessageSender(),
				parser, tapInstanceRepository());
	}

	@Bean
	public DeploymentMessageSender deploymentMessageSender() {
		return mock(DeploymentMessageSender.class);
	}


}
