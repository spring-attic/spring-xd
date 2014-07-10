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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.xd.dirt.job.NoSuchBatchJobInstanceException;
import org.springframework.xd.dirt.plugins.job.DistributedJobLocator;

/**
 * Tests REST compliance of {@link BatchJobInstancesController} endpoints.
 * 
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, Dependencies.class })
public class BatchJobInstancesControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Autowired
	private JobService jobService;

	@Autowired
	private DistributedJobLocator jobLocator;

	@Autowired
	private BatchJobInstancesController batchJobInstancesController;

	private JobExecution execution;

	@Before
	public void before() throws Exception {
		SimpleJob job1 = new SimpleJob("job1");
		SimpleJob job2 = new SimpleJob("job2");
		Collection<String> jobNames = new ArrayList<String>();
		jobNames.add(job1.getName());
		jobNames.add(job2.getName());
		Collection<JobInstance> jobInstances = new ArrayList<JobInstance>();
		JobInstance jobInstance1 = new JobInstance(0l, job1.getName());
		JobInstance jobInstance2 = new JobInstance(2l, job2.getName());
		JobInstance jobInstance3 = new JobInstance(3l, job1.getName());
		jobInstances.add(jobInstance1);
		jobInstances.add(jobInstance3);

		Map<String, JobParameter> parametersMap1 = new HashMap<String, JobParameter>();
		parametersMap1.put("param1", new JobParameter("test", true));
		parametersMap1.put("param2", new JobParameter(123l, false));
		JobParameters jobParameters1 = new JobParameters(parametersMap1);
		JobParameters jobParameters2 = new JobParameters(parametersMap1);
		JobExecution jobExecution1 = new JobExecution(jobInstance1, 0l, jobParameters1, null);
		JobExecution jobExecution2 = new JobExecution(jobInstance2, 3l, jobParameters2, null);

		// Verify XD-999
		StepExecution stepExecution = new StepExecution("s1", jobExecution2);
		List<StepExecution> stepExecutions = new ArrayList<StepExecution>();
		stepExecutions.add(stepExecution);
		jobExecution2.addStepExecutions(stepExecutions);

		Collection<JobExecution> jobExecutions1 = new ArrayList<JobExecution>();
		Collection<JobExecution> jobExecutions2 = new ArrayList<JobExecution>();
		jobExecutions1.add(jobExecution1);
		jobExecutions1.add(jobExecution2);
		jobExecutions2.add(jobExecution2);

		when(jobLocator.getJobNames()).thenReturn(jobNames);
		when(jobService.countJobExecutionsForJob(job1.getName())).thenReturn(2);
		when(jobService.countJobExecutionsForJob(job2.getName())).thenReturn(1);

		when(jobService.isLaunchable(job1.getName())).thenReturn(false);
		when(jobService.isLaunchable(job2.getName())).thenReturn(true);

		when(jobService.isIncrementable(job1.getName())).thenReturn(false);
		when(jobService.isIncrementable(job2.getName())).thenReturn(true);

		when(jobService.listJobInstances(job1.getName(), 0, 20)).thenReturn(jobInstances);

		Date startTime = new Date();
		Date endTime = new Date();
		Collection<JobExecution> jobExecutions = new ArrayList<JobExecution>();
		execution = new JobExecution(0L,
				new JobParametersBuilder().addString("foo", "bar").addLong("foo2", 0L).toJobParameters());
		execution.setExitStatus(ExitStatus.COMPLETED);
		execution.setStartTime(startTime);
		execution.setEndTime(endTime);
		jobExecutions.add(execution);

		when(jobService.getJobInstance(jobInstance1.getInstanceId())).thenReturn(jobInstance1);
		when(jobService.getJobExecutionsForJobInstance(jobInstance1.getJobName(), jobInstance1.getInstanceId())).thenReturn(
				jobExecutions2);
		when(jobService.getJobInstance(100)).thenThrow(new NoSuchBatchJobInstanceException(100));
		when(jobService.getJobInstance(101)).thenReturn(jobInstance2);
		when(jobService.getJobExecutionsForJobInstance("job2", jobInstance2.getInstanceId())).thenThrow(
				new NoSuchJobException("job2"));
	}

	@Test
	public void testGetJobInstanceByInstanceId() throws Exception {
		mockMvc.perform(
				get("/jobs/instances/0").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.instanceId").value(0))
				.andExpect(jsonPath("$.jobName").value("job1"))
				.andExpect(jsonPath("$.jobExecutions", Matchers.hasSize(1)))
				.andExpect(jsonPath("$.jobExecutions[0].jobExecution.id").value(3))
				.andExpect(jsonPath("$.jobExecutions[0].jobExecution.stepExecutions", Matchers.hasSize(1)))
				.andExpect(jsonPath("$.jobExecutions[0].jobExecution.stepExecutions[0].stepName").value("s1"));
	}

	@Test
	public void testGetJobInstanceByJobName() throws Exception {
		mockMvc.perform(
				get("/jobs/instances").param("jobname", "job1").param("startJobInstance", "0").param("pageSize", "20").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$", Matchers.hasSize(2)))
				.andExpect(jsonPath("$[*].instanceId", contains(0, 3)))
				.andExpect(jsonPath("$[*].jobName", contains("job1", "job1")));
	}

	@Test
	public void testGetJobInstanceByInvalidInstanceId() throws Exception {
		mockMvc.perform(
				get("/jobs/instances/100").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$[0].message", Matchers.is("Batch Job instance with the id 100 doesn't exist")));
	}

	@Test
	public void testGetJobInstanceExecutionsByInvalidJobName() throws Exception {
		mockMvc.perform(
				get("/jobs/instances/101").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$[0].message", Matchers.is("Batch Job with the name job2 doesn't exist")));
	}
}
