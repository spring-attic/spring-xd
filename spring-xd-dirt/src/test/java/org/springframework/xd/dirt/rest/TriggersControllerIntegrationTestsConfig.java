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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.xd.dirt.stream.XDStreamParser;
import org.springframework.xd.dirt.stream.TriggerDefinitionRepository;
import org.springframework.xd.dirt.stream.TriggerDeployer;
import org.springframework.xd.dirt.stream.XDParser;
import org.springframework.xd.dirt.stream.memory.InMemoryTriggerDefinitionRepository;

/**
 * @author Gunnar Hillert
 * @since 1.0
 * 
 */
@Configuration
public class TriggersControllerIntegrationTestsConfig extends Dependencies {

	@Bean
	public TriggerDefinitionRepository triggerDefinitionRepository() {
		return new InMemoryTriggerDefinitionRepository();
	}

	@Bean
	public TriggerDeployer triggerDeployer() {
		XDParser parser = new XDStreamParser(
				triggerDefinitionRepository(), moduleRegistry());

		return new TriggerDeployer(triggerDefinitionRepository(),
				deploymentMessageSender(), parser);
	}
}
