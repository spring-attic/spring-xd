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

import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.xd.dirt.plugins.job.BatchJobLocator;

/**
 * Tests REST compliance of BatchJobExecutionsController endpoints.
 * 
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, Dependencies.class })
public class BatchJobExecutionsControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Autowired
	private JobService jobService;

	@Autowired
	private BatchJobLocator jobLocator;

	@SuppressWarnings("unchecked")
	@Before
	public void before() throws Exception {
		SimpleJob job1 = new SimpleJob("job1.job");
		SimpleJob job2 = new SimpleJob("job2.job");
		Collection<String> jobNames = new ArrayList<String>();
		jobNames.add(job1.getName());
		jobNames.add(job2.getName());
		JobInstance jobInstance1 = new JobInstance(0l, job1.getName());
		JobInstance jobInstance2 = new JobInstance(2l, job2.getName());
		Map<String, JobParameter> parametersMap1 = new HashMap<String, JobParameter>();
		parametersMap1.put("param1", new JobParameter("test", true));
		parametersMap1.put("param2", new JobParameter(123l, false));
		JobParameters jobParameters1 = new JobParameters(parametersMap1);
		JobParameters jobParameters2 = new JobParameters(parametersMap1);
		JobExecution jobExecution1 = new JobExecution(jobInstance1, 0l, jobParameters1);
		JobExecution jobExecution2 = new JobExecution(jobInstance2, 3l, jobParameters2);
		Collection<JobExecution> jobExecutions1 = new ArrayList<JobExecution>();
		Collection<JobExecution> jobExecutions2 = new ArrayList<JobExecution>();
		StepExecution step1 = new StepExecution("step1", jobExecution1);
		StepExecution step2 = new StepExecution("step2", jobExecution1);
		List<StepExecution> stepExecutions1 = new ArrayList<StepExecution>();
		stepExecutions1.add(step1);
		stepExecutions1.add(step2);
		jobExecution1.addStepExecutions(stepExecutions1);
		StepExecution step3 = new StepExecution("step3", jobExecution2);
		List<StepExecution> stepExecutions2 = new ArrayList<StepExecution>();
		stepExecutions2.add(step3);
		jobExecution2.addStepExecutions(stepExecutions2);
		jobExecutions1.add(jobExecution1);
		jobExecutions1.add(jobExecution2);
		jobExecutions2.add(jobExecution2);
		when(jobLocator.getJobNames()).thenReturn(jobNames);
		when(jobService.listJobs(0, 20)).thenReturn(jobNames);
		when(jobService.countJobExecutionsForJob(job1.getName())).thenReturn(2);
		when(jobService.countJobExecutionsForJob(job2.getName())).thenReturn(1);

		when(jobService.isIncrementable(job1.getName())).thenReturn(false);
		when(jobService.isIncrementable(job2.getName())).thenReturn(true);

		when(jobService.listJobExecutions(0, 20)).thenReturn(jobExecutions1);
		when(jobService.listJobExecutionsForJob(job2.getName(), 0, 20)).thenReturn(jobExecutions2);
		when(jobService.getJobExecution(jobExecution1.getId())).thenReturn(jobExecution1);
		when(jobService.getJobExecution(99999L)).thenThrow(new NoSuchJobExecutionException("Not found."));

		when(jobService.stop(3l)).thenThrow(JobExecutionNotRunningException.class);
		when(jobService.stop(5l)).thenThrow(NoSuchJobExecutionException.class);

	}

	@Test
	public void testGetBatchJobExecutions() throws Exception {
		mockMvc.perform(
				get("/batch/executions").param("startJobExecution", "0").param("pageSize", "20").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(
				jsonPath("$", Matchers.hasSize(2))).andExpect(jsonPath("$[*].executionId", contains(0, 3))).andExpect(
				jsonPath("$[*].jobExecution[*].stepExecutions", Matchers.hasSize(3))).andExpect(
				jsonPath("$[*].jobId", contains(0, 2))).andExpect(jsonPath("$[*].jobExecution[*].id", contains(0, 3))).andExpect(
				jsonPath("$[*].jobExecution[*].jobParameters.parameters.param1.value", contains("test", "test"))).andExpect(
				jsonPath("$[*].jobExecution[*].jobParameters.parameters.param1.type", contains("STRING", "STRING"))).andExpect(
				jsonPath("$[*].jobExecution[*].jobParameters.parameters.param1.identifying", contains(true, true))).andExpect(
				jsonPath("$[*].jobExecution[*].jobParameters.parameters.param2.value", contains(123, 123))).andExpect(
				jsonPath("$[*].jobExecution[*].jobParameters.parameters.param2.type", contains("LONG", "LONG"))).andExpect(
				jsonPath("$[*].jobExecution[*].jobParameters.parameters.param2.identifying", contains(false, false)));
	}

	@Test
	public void testGetSingleBatchJobExecution() throws Exception {
		mockMvc.perform(
				get("/batch/executions/0").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.executionId", Matchers.is(0)))
				.andExpect(jsonPath("$.jobExecution.id", Matchers.is(0)))
				.andExpect(jsonPath("$.jobExecution.jobParameters.parameters.param1.type", Matchers.is("STRING")))
				.andExpect(jsonPath("$.jobExecution.jobParameters.parameters.param1.identifying", Matchers.is(true)))
				.andExpect(jsonPath("$.jobExecution.jobParameters.parameters.param1.value", Matchers.is("test")))
				.andExpect(jsonPath("$.jobExecution.jobParameters.parameters.param2.type", Matchers.is("LONG")))
				.andExpect(jsonPath("$.jobExecution.jobParameters.parameters.param2.identifying", Matchers.is(false)))
				.andExpect(jsonPath("$.jobExecution.jobParameters.parameters.param2.value", Matchers.is(123)))
				.andExpect(jsonPath("$.jobExecution.stepExecutions", Matchers.hasSize(2)))
				.andExpect(jsonPath("$.stepExecutionCount", Matchers.is(2)))
				.andExpect(jsonPath("$.name", Matchers.is("job1")));
	}

	@Test
	public void testGetNonExistingBatchJobExecution() throws Exception {
		mockMvc.perform(get("/batch/executions/99999").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$[0].message", Matchers.is("Could not find jobExecution with id '99999'")));
	}

	@Test
	public void testStopJobExecution() throws Exception {
		mockMvc.perform(delete("/batch/executions/{executionId}", "0")).andExpect(status().isOk());
	}

	@Test
	public void testStopJobExecutionNotRunning() throws Exception {
		mockMvc.perform(delete("/batch/executions/{executionId}", "3")).andExpect(status().isNotFound()).andExpect(
				jsonPath("$[0].message", Matchers.is("Job execution with executionId '3' is not running.")));
		;
	}

	@Test
	public void testStopJobExecutionNotExists() throws Exception {
		mockMvc.perform(delete("/batch/executions/{executionId}", "5")).andExpect(status().isNotFound()).andExpect(
				jsonPath("$[0].message",
						Matchers.is("Could not find jobExecution with id '5'")));

	}
}
