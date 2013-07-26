/*
 * Copyright 2013 the original author or authors.
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
 */

package org.springframework.xd.dirt.rest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.xd.analytics.metrics.core.CounterRepository;
import org.springframework.xd.analytics.metrics.core.FieldValueCounterRepository;
import org.springframework.xd.dirt.stream.JobDefinitionRepository;
import org.springframework.xd.dirt.stream.JobDeployer;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamDeployer;
import org.springframework.xd.dirt.stream.TapDefinitionRepository;
import org.springframework.xd.dirt.stream.TapDeployer;
import org.springframework.xd.dirt.stream.TriggerDefinitionRepository;
import org.springframework.xd.dirt.stream.TriggerDeployer;

import static org.mockito.Mockito.*;

/**
 * Provide a mockito mock for any of the business layer dependencies. Adding yet another
 * configuration class on top, one can selectively override those mocks (with <i>e.g.</i>
 * in memory implementations).
 * 
 * @author Eric Bottard
 * 
 */
@Configuration
public class MockedDependencies {

	@Bean
	public StreamDeployer streamDeployer() {
		return mock(StreamDeployer.class);
	}

	@Bean
	public TapDeployer tapDeployer() {
		return mock(TapDeployer.class);
	}

	@Bean
	public TriggerDeployer triggerDeployer() {
		return mock(TriggerDeployer.class);
	}

	@Bean
	public StreamDefinitionRepository streamDefinitionRepository() {
		return mock(StreamDefinitionRepository.class);
	}

	@Bean
	public TapDefinitionRepository tapDefinitionRepository() {
		return mock(TapDefinitionRepository.class);
	}

	@Bean
	public TriggerDefinitionRepository triggerDefinitionRepository() {
		return mock(TriggerDefinitionRepository.class);
	}

	@Bean
	public JobDeployer jobDeployer() {
		return mock(JobDeployer.class);
	}

	@Bean
	public JobDefinitionRepository jobDefinitionRepository() {
		return mock(JobDefinitionRepository.class);
	}

	@Bean
	public CounterRepository counterRepository() {
		return mock(CounterRepository.class);
	}

	@Bean
	public FieldValueCounterRepository fieldValueCounterRepository() {
		return mock(FieldValueCounterRepository.class);
	}
}
