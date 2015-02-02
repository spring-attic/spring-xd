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

import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.admin.service.NoSuchStepExecutionException;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.xd.dirt.plugins.job.DistributedJobLocator;

/**
 * Tests REST compliance of {@link BatchStepExecutionsController} endpoints.
 *
 * @author Gunnar Hillert
 * @since 1.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, Dependencies.class })
public class BatchStepExecutionsControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Autowired
	private JobService jobService;

	@Autowired
	private DistributedJobLocator jobLocator;

	@Before
	public void before() throws Exception {

		final SimpleJob job1 = new SimpleJob("job1.job");

		final JobInstance jobInstance1 = new JobInstance(0L, job1.getName());
		final Map<String, JobParameter> parametersMap1 = new HashMap<String, JobParameter>();
		parametersMap1.put("param1", new JobParameter("test", true));
		parametersMap1.put("param2", new JobParameter(123L, false));
		final JobParameters jobParameters1 = new JobParameters(parametersMap1);
		final JobExecution jobExecution1 = new JobExecution(jobInstance1, 2L, jobParameters1, null);

		final StepExecution step1 = new StepExecution("step1", jobExecution1, 1L);
		final StepExecution step2 = new StepExecution("step2", jobExecution1, 2L);
		final StepExecution step3 = new StepExecution("step3", jobExecution1, 3L);

		step1.getExecutionContext().put("contextTestKey", "howdy!");

		final List<StepExecution> stepExecutions1 = new ArrayList<StepExecution>();
		stepExecutions1.add(step1);
		stepExecutions1.add(step2);
		stepExecutions1.add(step3);

		final List<StepExecution> stepExecutions2 = new ArrayList<StepExecution>();
		stepExecutions2.add(step2);
		jobExecution1.addStepExecutions(stepExecutions1);

		when(jobService.getStepExecutions(5555L)).thenThrow(
				new NoSuchJobExecutionException("No JobExecution with id=5555L"));
		when(jobService.getStepExecutions(2L)).thenReturn(stepExecutions1);

		when(jobService.getStepExecution(2L, 1L)).thenReturn(step1);
		when(jobService.getStepExecution(5555L, 1L)).thenThrow(
				new NoSuchJobExecutionException("No JobExecution with id=5555L"));
		when(jobService.getStepExecution(2L, 5555L)).thenThrow(
				new NoSuchStepExecutionException("No StepExecution with id=5555L"));
		when(jobService.countStepExecutionsForStep(job1.getName(), step1.getStepName())).thenReturn(1);
		when(jobService.listStepExecutionsForStep(job1.getName(), step1.getStepName(), 0, 1000)).thenReturn(
				stepExecutions2);

	}

	@Test
	public void testGetBatchStepExecutions() throws Exception {
		mockMvc.perform(
				get("/jobs/executions/2/steps").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", Matchers.hasSize(3)))
				.andExpect(jsonPath("$[*].stepExecution.id", contains(1, 2, 3)))
				.andExpect(jsonPath("$[*].jobExecutionId", contains(2, 2, 2)))
				.andExpect(jsonPath("$[*].stepExecution.stepName", contains("step1", "step2", "step3")))
				.andExpect(jsonPath("$[*].links[*].href", contains(
						"http://localhost/jobs/executions/2/steps/1",
						"http://localhost/jobs/executions/2/steps/2",
						"http://localhost/jobs/executions/2/steps/3")));
	}

	@Test
	public void testGetBatchStepExecutionsNotExists() throws Exception {
		mockMvc.perform(get("/jobs/executions/{executionId}/steps", "5555")).andExpect(status().isNotFound()).andExpect(
				jsonPath("$[0].message",
						Matchers.is("Could not find jobExecution with id 5555")));

	}

	@Test
	public void testGetSingleBatchStepExecution() throws Exception {
		String s = mockMvc.perform(
				get("/jobs/executions/2/steps/1").accept(MediaType.APPLICATION_JSON)).andReturn().getResponse().getContentAsString();
		System.out.println(s);

		mockMvc.perform(
				get("/jobs/executions/2/steps/1").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("stepExecution.id", Matchers.is(1)))
				.andExpect(jsonPath("jobExecutionId", Matchers.is(2)))
				.andExpect(jsonPath("stepExecution.jobParameters.empty", Matchers.is(false)))
				.andExpect(jsonPath("stepExecution.jobParameters.parameters.param1.value", Matchers.is("test")))
				.andExpect(jsonPath("stepExecution.jobParameters.parameters.param2.value", Matchers.is(123)))
				.andExpect(jsonPath("stepExecution.stepName", Matchers.is("step1")))
				.andExpect(jsonPath("stepExecution.executionContext.empty", Matchers.is(false)))
				.andExpect(jsonPath("stepExecution.executionContext.values", Matchers.not(Matchers.empty())))
				.andExpect(jsonPath("stepExecution.executionContext.values", Matchers.hasSize(1)))
				.andExpect(
						jsonPath("stepExecution.executionContext.values[?(@.key == 'contextTestKey')].value",
								Matchers.hasSize(1)));
	}

	@Test
	public void testGetSingleBatchStepExecutionForNonExistingJobExecution() throws Exception {
		mockMvc.perform(get("/jobs/executions/{jobExecutionId}/steps/{stepExecutionId}", "5555", "1")).andExpect(
				status().isNotFound()).andExpect(
				jsonPath("$[0].message",
						Matchers.is("Could not find jobExecution with id 5555")));
	}

	@Test
	public void testGetSingleBatchStepExecutionThatDoesNotExist() throws Exception {
		mockMvc.perform(get("/jobs/executions/{jobExecutionId}/steps/{stepExecutionId}", "2", "5555")).andExpect(
				status().isNotFound()).andExpect(
				jsonPath("$[0].message",
						Matchers.is("Could not find step execution with id 5555")));
	}

	@Test
	public void testGetBatchStepExecutionProgress() throws Exception {
		mockMvc.perform(
				get("/jobs/executions/2/steps/1/progress").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stepExecution.id", Matchers.is(1)))
				.andExpect(jsonPath("$.percentageComplete", Matchers.is(0.5)));
	}

	@Test
	public void testGetProgressForJobExecutionNotExists() throws Exception {
		mockMvc.perform(get("/jobs/executions/{jobExecutionId}/steps/{stepExecutionId}/progress", "5555", "1")).andExpect(
				status().isNotFound()).andExpect(
				jsonPath("$[0].message",
						Matchers.is("Could not find jobExecution with id 5555")));
	}

	@Test
	public void testGetProgressForStepExecutionNotExists() throws Exception {
		mockMvc.perform(get("/jobs/executions/{jobExecutionId}/steps/{stepExecutionId}/progress", "2", "5555")).andExpect(
				status().isNotFound()).andExpect(
				jsonPath("$[0].message",
						Matchers.is("Could not find step execution with id 5555")));
	}
}
