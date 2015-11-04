/*
 * Copyright 2013-2014 the original author or authors.
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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.xd.dirt.core.DeploymentUnitStatus;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.module.WritableModuleRegistry;
import org.springframework.xd.dirt.plugins.job.DistributedJobLocator;
import org.springframework.xd.dirt.stream.JobDefinition;
import org.springframework.xd.dirt.stream.JobDefinitionRepository;
import org.springframework.xd.dirt.stream.JobRepository;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.TestModuleDefinitions;

/**
 * Tests REST compliance of jobs-related endpoints.
 *
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 * @author Florent Biville
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {RestConfiguration.class, Dependencies.class})
public class JobsControllerIntegrationTests extends AbstractControllerIntegrationTest {

	private static final String JOB_DEFINITION = "job --cron='*/10 * * * * *'";

	@Autowired
	private ModuleRegistry moduleRegistry;
	
	@Autowired
	private WritableModuleRegistry writeableModuleRegistry;

	@Autowired
	private JobDefinitionRepository jobDefinitionRepository;

	@Autowired
	private JobRepository xdJobRepository;

	@Autowired
	private DistributedJobLocator jobLocator;

	@Before
	public void before() {
		ModuleDefinition moduleJobDefinition = TestModuleDefinitions.dummy("job", ModuleType.job);
		ArrayList<ModuleDefinition> moduleDefinitions = new ArrayList<ModuleDefinition>();
		moduleDefinitions.add(moduleJobDefinition);
		when(moduleRegistry.findDefinitions("job")).thenReturn(moduleDefinitions);
		when(moduleRegistry.findDefinitions("job1")).thenReturn(moduleDefinitions);
		when(moduleRegistry.findDefinitions("job2")).thenReturn(moduleDefinitions);
		when(moduleRegistry.findDefinition("job1", ModuleType.job)).thenReturn(moduleJobDefinition);
		when(moduleRegistry.findDefinition("job2", ModuleType.job)).thenReturn(moduleJobDefinition);
		when(moduleRegistry.findDefinition("job", ModuleType.job)).thenReturn(moduleJobDefinition);
		when(jobLocator.getJobNames()).thenReturn(Arrays.asList(new String[] {}));
		ModuleDefinition any = Mockito.any();
		when(writeableModuleRegistry.registerNew(any)).thenReturn(true);
	}

	@After
	public void cleanUp() {
		jobDefinitionRepository.deleteAll();
		xdJobRepository.deleteAll();
	}

	@Test
	public void testSuccessfulJobCreation() throws Exception {
		mockMvc.perform(
				post("/jobs/definitions").param("name", "job1").param("definition", JOB_DEFINITION).accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
	}

	@Test
	public void testComposedJobCreation() throws Exception {
		mockMvc.perform(
				post("/jobs/definitions").param("name", "job1").param("definition", "A || B").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
	}

	@Test
	public void testComposedJobCreationBadDSL() throws Exception {
		mockMvc.perform(
				post("/jobs/definitions").param("name", "job1").param("definition", "A || B >").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isInternalServerError());
	}
	
	@Test
	public void testSuccessfulJobCreateAndDeploy() throws Exception {
		mockMvc.perform(
				post("/jobs/definitions").param("name", "job5").param("definition", JOB_DEFINITION).accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		JobDefinition jobDefinition = jobDefinitionRepository.findOne("job5");
		assertNotNull(jobDefinition);
		assertEquals("job5", jobDefinition.getName());
		assertNotNull(xdJobRepository.findOne(jobDefinition.getName()));
	}

	@Test
	public void testSuccessfulJobDeletion() throws Exception {
		mockMvc.perform(delete("/jobs/definitions/{name}", "job1"));
		mockMvc.perform(
				post("/jobs/definitions").param("name", "job1").param("definition", JOB_DEFINITION).accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		mockMvc.perform(delete("/jobs/definitions/{name}", "job1")).andExpect(status().isOk());
	}

	@Test
	public void testListAllJobs() throws Exception {
		mockMvc.perform(
				post("/jobs/definitions").param("name", "job1").param("definition", JOB_DEFINITION).accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		mockMvc.perform(
				post("/jobs/definitions").param("name", "job2").param("definition", JOB_DEFINITION).accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		mockMvc.perform(get("/jobs/definitions").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(2))).andExpect(jsonPath("$.content[0].name").value("job1")).andExpect(
				jsonPath("$.content[1].name").value("job2"));
	}

	@Test
	public void testJobCreationNoDefinition() throws Exception {
		mockMvc.perform(post("/jobs/definitions").param("name", "myjob").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isBadRequest());
	}

	@Test
	public void testJobUnDeployNoDef() throws Exception {
		mockMvc.perform(
				post("/jobs/definitions").param("name", "job1").param("definition", JOB_DEFINITION).accept(
						MediaType.APPLICATION_JSON).param("deploy", "false")).andExpect(status().isCreated());
		mockMvc.perform(delete("/jobs/deployments/{name}", "myjob").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isNotFound());
	}

	@Test
	public void testJobDeployNoDef() throws Exception {
		mockMvc.perform(post("/jobs/deployments/myjob").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isNotFound());
	}

	@Test
	public void testCreateOnAlreadyCreatedJob() throws Exception {
		mockMvc.perform(
				post("/jobs/definitions").param("name", "job1").param("definition", JOB_DEFINITION).accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		mockMvc.perform(
				post("/jobs/definitions").param("name", "job1").param("definition", JOB_DEFINITION).accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
	}

	@Test
	public void testFailedJobDeletion() throws Exception {
		mockMvc.perform(delete("/jobs/definitions/{name}", "job1")).andExpect(status().isNotFound());
	}

	@Test
	public void testInvalidDefinitionCreate() throws Exception {
		mockMvc.perform(
				post("/jobs/definitions").param("name", "job1").param("definition", "job adsfa").accept(
						MediaType.APPLICATION_JSON)).andExpect(
				status().isBadRequest());

		mockMvc.perform(get("/jobs/definitions").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(0)));
	}

	@Test
	public void testJobDestroyAll() throws Exception {
		mockMvc.perform(
				post("/jobs/definitions").param("name", "job1").param("definition", JOB_DEFINITION).accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		mockMvc.perform(
				post("/jobs/definitions").param("name", "job2").param("definition", JOB_DEFINITION).accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		assertNotNull(jobDefinitionRepository.findOne("job1"));
		assertNotNull(jobDefinitionRepository.findOne("job2"));

		// Perform destroy all
		mockMvc.perform(delete("/jobs/definitions").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

		assertNull(jobDefinitionRepository.findOne("job1"));
		assertNull(jobDefinitionRepository.findOne("job2"));
	}

	@Test
	public void testJobThatAlreadyExistsInJobRepo() throws Exception {
		when(jobLocator.getJobNames()).thenReturn(Arrays.asList(new String[] {"mydupejob"}));
		mockMvc.perform(
				post("/jobs/definitions").param("name", "mydupejob").param("definition", "job adsfa").accept(
						MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest()).andExpect(
				jsonPath("$[0].message", Matchers.is("Batch Job with the name mydupejob already exists")));
	}

	@Test
	public void testCreatedUndeployedJobIsExposedAsUndeployed() throws Exception {
		mockMvc.perform(
				post("/jobs/definitions").param("name", "job1").param("definition", JOB_DEFINITION).param("deploy",
						"false")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		mockMvc.perform(get("/jobs/definitions/job1")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", equalTo(DeploymentUnitStatus.State.undeployed.toString())));

	}

	@Test
	public void testCreatedAndDeployedJobIsExposedAsDeployed() throws Exception {
		mockMvc.perform(
				post("/jobs/definitions").param("name", "job1").param("definition", JOB_DEFINITION).param("deploy",
						"true")
						.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		mockMvc.perform(get("/jobs/definitions/job1")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", equalTo(DeploymentUnitStatus.State.deploying.toString())));

	}
}
