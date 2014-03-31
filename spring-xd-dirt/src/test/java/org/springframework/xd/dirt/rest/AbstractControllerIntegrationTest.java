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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.Before;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;

import org.springframework.batch.admin.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.xd.analytics.metrics.core.CounterRepository;
import org.springframework.xd.analytics.metrics.core.FieldValueCounterRepository;
import org.springframework.xd.analytics.metrics.core.GaugeRepository;
import org.springframework.xd.analytics.metrics.core.RichGaugeRepository;
import org.springframework.xd.dirt.container.store.ContainerMetadataRepository;
import org.springframework.xd.dirt.module.store.ModuleMetadataRepository;
import org.springframework.xd.dirt.rest.AbstractControllerIntegrationTest.LegacyMvcConfiguration;
import org.springframework.xd.dirt.stream.JobDefinitionRepository;
import org.springframework.xd.dirt.stream.JobDeployer;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamDeployer;

/**
 * Base class for Controller layer tests. Takes care of resetting the mocked (be them mockito mocks or <i>e.g.</i> in
 * memory) dependencies before each test.
 * 
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
@ContextConfiguration(classes = { LegacyMvcConfiguration.class })
public class AbstractControllerIntegrationTest {

	private MockUtil mockUtil = new MockUtil();

	protected MockMvc mockMvc;

	@Configuration
	@EnableWebMvc
	protected static class LegacyMvcConfiguration {
	}

	@Autowired
	private WebApplicationContext wac;

	@Before
	public void setupMockMVC() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).defaultRequest(
				get("/").accept(MediaType.APPLICATION_JSON)).build();
	}

	// Deployers
	@Autowired
	protected StreamDeployer streamDeployer;

	@Autowired
	protected JobDeployer jobDeployer;

	// Definition Repositories
	@Autowired
	protected StreamDefinitionRepository streamDefinitionRepository;

	@Autowired
	protected JobDefinitionRepository jobDefinitionRepository;

	// Container Metadata Repository
	@Autowired
	protected ContainerMetadataRepository containerMetadataRepository;

	// Module Metadata Repository
	@Autowired
	protected ModuleMetadataRepository moduleMetadataRepository;

	// Analytics repositories
	@Autowired
	protected CounterRepository counterRepository;

	@Autowired
	protected FieldValueCounterRepository fieldValueCounterRepository;

	@Autowired
	protected GaugeRepository gaugeRepository;

	@Autowired
	protected RichGaugeRepository richGaugeRepository;

	@Autowired
	private JobService jobService;

	@Before
	public void resetDependencies() {
		maybeReset(streamDeployer);
		maybeReset(jobDeployer);
		maybeReset(jobService);

		resetOrDelete(containerMetadataRepository);
		resetOrDelete(moduleMetadataRepository);

		resetOrDelete(streamDefinitionRepository);
		resetOrDelete(jobDefinitionRepository);

		resetOrDelete(counterRepository);
		resetOrDelete(fieldValueCounterRepository);
		resetOrDelete(gaugeRepository);
		resetOrDelete(richGaugeRepository);
	}

	/**
	 * Conditional resetting in case the dependency has been overridden and is not actually a mock.
	 */
	private void maybeReset(Object o) {
		if (mockUtil.isMock(o)) {
			Mockito.reset(o);
		}
	}

	private void resetOrDelete(CrudRepository<?, ?> repo) {
		if (mockUtil.isMock(repo)) {
			Mockito.reset(new Object[] { repo });
		}
		else {
			repo.deleteAll();
		}
	}

}
