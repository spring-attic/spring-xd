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

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.stream.JobDefinition;
import org.springframework.xd.dirt.stream.JobDefinitionRepository;
import org.springframework.xd.dirt.stream.JobDeployer;
import org.springframework.xd.dirt.stream.JobDeploymentMessageSender;

/**
 * Tests REST compliance of jobs-related endpoints.
 * 
 * @author Glenn Renfro
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, MockedDependencies.class, JobsControllerTestsConfig.class })
public class JobsControllerTests {

	@Autowired
	private JobDefinitionRepository repository;

	@Autowired
	private JobDeploymentMessageSender sender;

	@Autowired
	private JobDeployer jobDeployer;

	@Autowired
	private JobController jobsController;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Before
	public void setupMockMVC() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
	}


	@Test
	public void testSuccessfulJobCreation() throws Exception {
		mockMvc.perform(
				post("/jobs").param("name", "job1").param("definition", "Job --cron='*/10 * * * * *'")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
	}

	@Test
	public void testSuccessfulJobCreateAndDeploy() throws Exception {
		mockMvc.perform(
				post("/jobs").param("name", "job2").param("definition","Job --cron='*/10 * * * * *'")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		verify(sender, times(1)).sendDeploymentRequests(eq("job2"), anyListOf(ModuleDeploymentRequest.class));
	}



	@After
	public void clearRepos() {
		repository.deleteAll();

	}
}
