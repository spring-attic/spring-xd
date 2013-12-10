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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
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
import java.util.TimeZone;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.xd.dirt.job.JobExecutionInfo;
import org.springframework.xd.dirt.plugins.job.BatchJobLocator;

/**
 * Tests REST compliance of BatchJobsController endpoints.
 * 
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, Dependencies.class })
public class BatchJobsControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Autowired
	private JobService jobService;

	@Autowired
	private BatchJobLocator jobLocator;

	@Autowired
	private BatchJobsController batchJobsController;

	private JobExecution execution;

	private TimeZone timeZone = TimeZone.getDefault();

	@Before
	public void before() throws Exception {
		SimpleJob job1 = new SimpleJob("job1.job");
		SimpleJob job2 = new SimpleJob("job2.job");
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
		when(jobService.listJobs(0, 20)).thenReturn(jobNames);
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

		when(jobService.listJobExecutionsForJob(job1.getName(), 0, 1)).thenReturn(jobExecutions);
		when(jobService.listJobExecutions(0, 20)).thenReturn(jobExecutions1);
		when(jobService.listJobExecutionsForJob(job2.getName(), 0, 20)).thenReturn(jobExecutions2);

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetBatchJobs() throws Exception {
		JobExecutionInfo info = new JobExecutionInfo(execution, timeZone);
		mockMvc.perform(
				get("/batch/jobs").param("startJob", "0").param("pageSize", "20").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(jsonPath("$", Matchers.hasSize(2))).andExpect(
				jsonPath("$[*].executionCount", contains(2, 1))).andExpect(
				jsonPath("$[*].launchable", contains(false, true))).andExpect(
				jsonPath("$[*].incrementable", contains(false, true))).andExpect(
				jsonPath("$[*].jobInstanceId", contains(nullValue(), nullValue()))).andExpect(
				jsonPath("$[*].duration", contains(info.getDuration(), null))).andExpect(
				jsonPath("$[*].startTime", contains(info.getStartTime(), null))).andExpect(
				jsonPath("$[*].startDate", contains(info.getStartDate(), null))).andExpect(
				jsonPath("$[*].stepExecutionCount", contains(info.getStepExecutionCount(), 0))).andExpect(
				jsonPath("$[*].jobParameters", contains(info.getJobParametersString(), null)))

				// should contain the display name (ie- without the .job suffix)
				.andExpect(jsonPath("$[0].name", equalTo("job1"))).andExpect(
						jsonPath("$[0].jobInstanceId", nullValue()))

				.andExpect(jsonPath("$[1].name", equalTo("job2"))).andExpect(
						jsonPath("$[1].jobInstanceId", nullValue()))

				// exit status is non null for job 0 and null for job 1
				.andExpect(
						jsonPath("$[0].exitStatus.exitDescription",
								equalTo(execution.getExitStatus().getExitDescription()))).andExpect(
						jsonPath("$[0].exitStatus.exitCode", equalTo(execution.getExitStatus().getExitCode()))).andExpect(
						jsonPath("$[0].exitStatus.running", equalTo(false))).andExpect(
						jsonPath("$[1].exitStatus", nullValue()));
	}

	@Test
	public void testGetJobInstanceByJobName() throws Exception {
		mockMvc.perform(
				get("/batch/jobs/job1/instances").param("startJobInstance", "0").param("pageSize", "20").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$", Matchers.hasSize(2)))
				.andExpect(jsonPath("$[*].id", contains(0, 3)))
				.andExpect(jsonPath("$[*].jobName", contains("job1", "job1")));
	}

	@Test
	public void testGetJobInfoByJobName() throws Exception {
		mockMvc.perform(
				get("/batch/jobs/job1").param("startJobInstance", "0").param("pageSize", "20").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(
				jsonPath("$.executionCount").value(2)).andExpect(jsonPath("$.launchable").value(false)).andExpect(
				jsonPath("$.incrementable").value(false)).andExpect(jsonPath("$.jobInstanceId", nullValue()));

	}

	@Test
	public void testGetJobExecutionsByName() throws Exception {
		mockMvc.perform(
				get("/batch/jobs/job2/executions").param("startJobExecution", "0").param("pageSize", "20").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$", Matchers.hasSize(1)))
				.andExpect(jsonPath("$[0].executionId").value(3))
				.andExpect(jsonPath("$[0].jobId").value(2))
				.andExpect(jsonPath("$[0].jobExecution[*].id").value(3))
				.andExpect(
						jsonPath("$[0].jobExecution[*].jobParameters.parameters.param1.value").value("test"))
				.andExpect(
						jsonPath("$[0].jobExecution[*].jobParameters.parameters.param1.type").value("STRING"))
				.andExpect(jsonPath("$[0].jobExecution[*].jobParameters.parameters.param1.identifying").value(
						true))
				.andExpect(
						jsonPath("$[0].jobExecution[*].jobParameters.parameters.param2.value").value(123))
				.andExpect(
						jsonPath("$[0].jobExecution[*].jobParameters.parameters.param2.type").value("LONG"))
				.andExpect(jsonPath("$[0].jobExecution[*].jobParameters.parameters.param2.identifying").value(
						false));
	}
}
