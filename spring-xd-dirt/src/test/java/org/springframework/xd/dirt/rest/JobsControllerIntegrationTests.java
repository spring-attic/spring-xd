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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.stream.JobDeploymentMessageSender;

/**
 * Tests REST compliance of jobs-related endpoints.
 * 
 * @author Glenn Renfro
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, MockedDependencies.class, JobsControllerIntegrationTestsConfig.class })
public class JobsControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Autowired
	private JobDeploymentMessageSender sender;

	@Test
	public void testSuccessfulJobCreation() throws Exception {
		mockMvc.perform(
				post("/jobs").param("name", "job1").param("definition", "Job --cron='*/10 * * * * *'")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
	}

	@Test
	public void testSuccessfulJobCreateAndDeploy() throws Exception {
		mockMvc.perform(
				post("/jobs").param("name", "job5").param("definition", "Job --cron='*/10 * * * * *'")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		verify(sender, times(1)).sendDeploymentRequests(eq("job5"), anyListOf(ModuleDeploymentRequest.class));
	}
	@Test
	public void testSuccessfulJobDeletion() throws Exception {
		mockMvc.perform(
				post("/jobs").param("name", "job1").param("definition", "Job --cron='*/10 * * * * *'")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		mockMvc.perform(delete("/jobs/{name}", "job1")).andExpect(status().isOk());
	}
	@Test
	public void testListAllTaps() throws Exception {
		mockMvc.perform(
				post("/jobs").param("name", "job1").param("definition", "Job --cron='*/10 * * * * *'")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		mockMvc.perform(
				post("/jobs").param("name", "job2").param("definition", "Job --cron='*/10 * * * * *'")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		mockMvc.perform(get("/jobs").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$", Matchers.hasSize(2))).andExpect(jsonPath("$.[0].name").value("job1"))
				.andExpect(jsonPath("$.[1].name").value("job2"));
	}

	@Test
	public void testJobCreationNoDefinition() throws Exception {
		mockMvc.perform(post("/jobs").param("name", "myjob").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isBadRequest());
	}
	@Test
	public void testJobUnDeployNoDef() throws Exception {
		mockMvc.perform(put("/jobs/{name}","myjob").param("deploy", "false").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isNotFound());
	}
	@Test
	public void testJobDeployNoDef() throws Exception {
		mockMvc.perform(put("/jobs/{name}","myjob").param("deploy", "true").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isNotFound());
	}
	@Test
	public void testCreateOnAlreadyCreatedJob() throws Exception {
		mockMvc.perform(
				post("/jobs").param("name", "job1").param("definition", "Job --cron='*/10 * * * * *'")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		mockMvc.perform(
				post("/jobs").param("name", "job1").param("definition", "Job --cron='*/10 * * * * *'")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
	}
	@Test
	public void testFailedJobDeletion() throws Exception {
		mockMvc.perform(delete("/jobs/{name}", "job1")).andExpect(status().isNotFound());
	}
}
