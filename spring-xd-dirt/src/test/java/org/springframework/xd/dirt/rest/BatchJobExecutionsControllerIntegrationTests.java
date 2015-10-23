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
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.DefaultJobParametersValidator;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.plugins.job.DistributedJobLocator;
import org.springframework.xd.dirt.plugins.job.DistributedJobService;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.TestModuleDefinitions;

/**
 * Tests REST compliance of {@link BatchJobExecutionsController} endpoints.
 *
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {RestConfiguration.class, Dependencies.class})
public class BatchJobExecutionsControllerIntegrationTests extends AbstractControllerIntegrationTest {

	private static final String JOB_DEFINITION = "job --cron='*/10 * * * * *'";

	@Autowired
	private DistributedJobService jobService;

	@Autowired
	private DistributedJobLocator jobLocator;

	@Autowired
	private ModuleRegistry moduleRegistry;

	@Autowired
	private MessageBus messageBus;

	@SuppressWarnings("unchecked")
	@Before
	public void before() throws Exception {
		ModuleDefinition moduleJobDefinition = TestModuleDefinitions.dummy("job", ModuleType.job);
		ArrayList<ModuleDefinition> moduleDefinitions = new ArrayList<ModuleDefinition>();
		moduleDefinitions.add(moduleJobDefinition);
		when(moduleRegistry.findDefinitions("job")).thenReturn(moduleDefinitions);
		when(moduleRegistry.findDefinition("job", ModuleType.job)).thenReturn(moduleJobDefinition);
		SimpleJob job1 = new SimpleJob("job1");
		SimpleJob job2 = new SimpleJob("job2");
		job2.setRestartable(false);

		SimpleJob job3 = new SimpleJob("job3complete");
		SimpleJob job4 = new SimpleJob("job4running");
		SimpleJob job5 = new SimpleJob("job5with-null-parameters");
		Collection<String> jobNames = new ArrayList<String>();
		jobNames.add(job1.getName());
		jobNames.add(job2.getName());
		jobNames.add(job3.getName());
		jobNames.add(job4.getName());
		jobNames.add(job5.getName());
		JobInstance jobInstance1 = new JobInstance(0l, job1.getName());
		JobInstance jobInstance2 = new JobInstance(2l, job2.getName());
		JobInstance jobInstance3 = new JobInstance(3l, job3.getName());
		JobInstance jobInstance4 = new JobInstance(4l, job4.getName());
		JobInstance jobInstance5 = new JobInstance(5l, job5.getName());
		Map<String, JobParameter> parametersMap1 = new HashMap<String, JobParameter>();
		parametersMap1.put("param1", new JobParameter("test", true));
		parametersMap1.put("param2", new JobParameter(123l, false));

		DefaultJobParametersValidator jobParametersValidator = new DefaultJobParametersValidator();
		String[] requiredStrings = new String[] {"myRequiredString"};
		jobParametersValidator.setRequiredKeys(requiredStrings);
		job5.setJobParametersValidator(jobParametersValidator);

		Map<String, JobParameter> parametersMap5 = new HashMap<String, JobParameter>();
		parametersMap5.put("somthingElse", new JobParameter("test", true));


		JobParameters jobParameters1 = new JobParameters(parametersMap1);
		JobParameters jobParameters2 = new JobParameters(parametersMap1);
		JobParameters jobParameters3 = new JobParameters(parametersMap1);
		JobParameters jobParameters5 = new JobParameters(parametersMap5);
		JobExecution jobExecution1 = new JobExecution(jobInstance1, 0l, jobParameters1, null);
		JobExecution jobExecution2 = new JobExecution(jobInstance2, 3l, jobParameters2, null);
		JobExecution jobExecution3 = new JobExecution(jobInstance3, 33l, jobParameters3, null);
		JobExecution jobExecution4 = new JobExecution(jobInstance4, 4l, jobParameters3, null);
		JobExecution jobExecution5 = new JobExecution(jobInstance5, 5l, jobParameters5, null);

		BatchStatus status = BatchStatus.COMPLETED;
		jobExecution3.setStatus(status);

		jobExecution2.setEndTime(new Date());
		jobExecution3.setEndTime(new Date());
		jobExecution5.setEndTime(new Date());

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
		jobExecution3.addStepExecutions(stepExecutions2);
		jobExecution4.addStepExecutions(stepExecutions2);
		jobExecutions1.add(jobExecution1);
		jobExecutions1.add(jobExecution2);
		jobExecutions2.add(jobExecution2);
		// all the jobs are undeployed/deleted.
		when(jobLocator.getJobNames()).thenReturn(Collections.<String>emptyList());
		when(jobLocator.isRestartable("job1")).thenReturn(true);
		when(jobService.listJobs(0, 20)).thenReturn(jobNames);
		when(jobService.countJobExecutionsForJob(job1.getName())).thenReturn(2);
		when(jobService.countJobExecutionsForJob(job2.getName())).thenReturn(1);

		when(jobService.isIncrementable(job1.getName())).thenReturn(false);
		when(jobService.isIncrementable(job2.getName())).thenReturn(true);

		when(jobService.getTopLevelJobExecutions(5, 5)).thenReturn(jobExecutions1);
		when(jobService.getTopLevelJobExecutions(0, 20)).thenReturn(jobExecutions1);
		when(jobService.listJobExecutionsForJob(job2.getName(), 0, 20)).thenReturn(jobExecutions2);
		when(jobService.getJobExecution(jobExecution1.getId())).thenReturn(jobExecution1);
		when(jobService.getJobExecution(99999L)).thenThrow(new NoSuchJobExecutionException("Not found."));

		when(jobService.stop(3l)).thenThrow(JobExecutionNotRunningException.class);
		when(jobService.stop(5l)).thenThrow(NoSuchJobExecutionException.class);
		when(jobService.getJobExecution(1234l)).thenThrow(NoSuchJobExecutionException.class);

		when(jobService.getJobExecution(2L)).thenReturn(jobExecution2);
		when(jobLocator.getJob("job2")).thenReturn(job2);

		when(jobService.getJobExecution(33L)).thenReturn(jobExecution3);
		when(jobLocator.getJob("job3complete")).thenReturn(job3);
		when(jobService.getJobExecution(4L)).thenReturn(jobExecution4);
		when(jobService.getJobExecution(5L)).thenReturn(jobExecution5);
		when(jobLocator.getJob("job5with-null-parameters")).thenReturn(job5);
		when(jobService.getJobExecution(3333L)).thenThrow(NoSuchJobExecutionException.class);

	}

	@Test
	public void testGetJobExecutionsByName() throws Exception {
		mockMvc.perform(
				get("/jobs/executions").param("jobname", "job2")
						.param("startJobExecution", "0").param("pageSize", "20").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$", Matchers.hasSize(1)))
				.andExpect(jsonPath("$[0].executionId").value(3))
				.andExpect(jsonPath("$[0].jobId").value(2))
				.andExpect(jsonPath("$[0].jobExecution.id").value(3))
				.andExpect(
						jsonPath("$[0].jobExecution.jobParameters.parameters.param1.value").value("test"))
				.andExpect(
						jsonPath("$[0].jobExecution.jobParameters.parameters.param1.type").value("STRING"))
				.andExpect(jsonPath("$[0].jobExecution.jobParameters.parameters.param1.identifying").value(
						true))
				.andExpect(
						jsonPath("$[0].jobExecution.jobParameters.parameters.param2.value").value(123))
				.andExpect(
						jsonPath("$[0].jobExecution.jobParameters.parameters.param2.type").value("LONG"))
				.andExpect(jsonPath("$[0].jobExecution.jobParameters.parameters.param2.identifying").value(
						false));
	}

	@Test
	public void testGetBatchJobExecutions() throws Exception {
		mockMvc.perform(
				get("/jobs/executions").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(
						jsonPath("$.content", Matchers.hasSize(2))).andExpect(jsonPath("$.content[*].executionId", contains(0, 3))).andExpect(
				jsonPath("$.content[*].jobExecution.stepExecutions[*]", Matchers.hasSize(3))).andExpect(
				jsonPath("$.content[*].jobId", contains(0, 2))).andExpect(jsonPath("$.content[*].deleted", contains(true, true))).andExpect(
				jsonPath("$.content[*].deployed", contains(false, false))).andExpect(
				jsonPath("$.content[*].jobExecution.id", contains(0, 3))).andExpect(
				jsonPath("$.content[*].jobExecution.jobParameters.parameters.param1.value", contains("test", "test"))).andExpect(
				jsonPath("$.content[*].jobExecution.jobParameters.parameters.param1.type", contains("STRING", "STRING"))).andExpect(
				jsonPath("$.content[*].jobExecution.jobParameters.parameters.param1.identifying", contains(true, true))).andExpect(
				jsonPath("$.content[*].jobExecution.jobParameters.parameters.param2.value", contains(123, 123))).andExpect(
				jsonPath("$.content[*].jobExecution.jobParameters.parameters.param2.type", contains("LONG", "LONG"))).andExpect(
				jsonPath("$.content[*].jobExecution.jobParameters.parameters.param2.identifying", contains(false, false)));
	}

	@Test
	public void testGetBatchJobExecutionsPaginated() throws Exception {
		mockMvc.perform(
				get("/jobs/executions").param("page", "1").param("size", "5").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(
				jsonPath("$.content[*]", Matchers.hasSize(2)));
	}

	@Test
	public void testGetSingleBatchJobExecution() throws Exception {
		mockMvc.perform(
				get("/jobs/executions/0").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
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
		mockMvc.perform(get("/jobs/executions/99999").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$[0].message", Matchers.is("Could not find jobExecution with id 99999")));
	}

	@Test
	public void testSuccessfulJobLaunch() throws Exception {
		QueueChannel channel = new QueueChannel();
		messageBus.bindConsumer("job:joblaunch", channel, null);
		mockMvc.perform(
				post("/jobs/definitions").param("name", "joblaunch").param("definition", JOB_DEFINITION).accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		mockMvc.perform(
				post("/jobs/executions").param("jobname", "joblaunch").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isCreated());
		assertNotNull(channel.receive(3000));
	}

	@Test
	public void testStopAllJobExecutions() throws Exception {
		mockMvc.perform(put("/jobs/executions?stop=true")).andExpect(status().isOk());
	}

	@Test
	public void testStopJobExecution() throws Exception {
		mockMvc.perform(put("/jobs/executions/{executionId}?stop=true", "0")).andExpect(status().isOk());
	}

	@Test
	public void testRestartNonExistingJobExecution() throws Exception {
		mockMvc.perform(put("/jobs/executions/{executionId}?restart=true", "1234")).andExpect(status().isNotFound()).andExpect(
				jsonPath("$[0].message", Matchers.is("Could not find jobExecution with id 1234")));
	}

	@Test
	public void testRestartAlreadyRunningJobExecution() throws Exception {
		mockMvc.perform(put("/jobs/executions/{executionId}?restart=true", "4")).andExpect(status().isBadRequest()).andExpect(
				jsonPath(
						"$[0].message",
						Matchers.is("Job Execution for this job is already running: JobInstance: id=4, version=null, Job=[job4running]")));
	}

	@Test
	public void testRestartAlreadyCompleteJobExecution() throws Exception {
		mockMvc.perform(put("/jobs/executions/{executionId}?restart=true", "33")).andExpect(status().isBadRequest()).andExpect(
				jsonPath("$[0].message", Matchers.is("Job Execution 33 is already complete.")));
	}

	@Test
	public void testRestartJobExecutionWithJobNotAvailable() throws Exception {
		mockMvc.perform(put("/jobs/executions/{executionId}?restart=true", "3333")).andExpect(status().isNotFound()).andExpect(
				jsonPath("$[0].message", Matchers.is("Could not find jobExecution with id 3333")));
	}

	@Test
	public void testRestartJobExecutionWithInvalidJobParameters() throws Exception {
		mockMvc.perform(put("/jobs/executions/{executionId}?restart=true", "5")).andExpect(status().isBadRequest()).andExpect(
				jsonPath("$[0].message", Matchers.is("The Job Parameters for Job Execution 5 are invalid.")));
	}

	@Test
	public void testRestartNonRestartableJob() throws Exception {
		mockMvc.perform(put("/jobs/executions/{executionId}?restart=true", "2")).andExpect(status().isBadRequest()).andExpect(
				jsonPath("$[0].message", Matchers.is("The job 'job2' is not restartable.")));
	}


	@Test
	public void testStopJobExecutionNotRunning() throws Exception {
		mockMvc.perform(put("/jobs/executions/{executionId}?stop=true", "3")).andExpect(status().isNotFound()).andExpect(
				jsonPath("$[0].message", Matchers.is("Job execution with executionId 3 is not running.")));
	}

	@Test
	public void testStopJobExecutionNotExists() throws Exception {
		mockMvc.perform(put("/jobs/executions/{executionId}?stop=true", "5")).andExpect(status().isNotFound()).andExpect(
				jsonPath("$[0].message",
						Matchers.is("Could not find jobExecution with id 5")));
	}
}
