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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.xd.dirt.plugins.job.batch.BatchJobLocator;

/**
 * Tests REST compliance of BatchJobExecutionsController endpoints.
 * 
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, Dependencies.class })
public class BatchJobExecutionsControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Autowired
	private JobService jobService;

	@Autowired
	private BatchJobLocator jobLocator;

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
		jobInstances.add(jobInstance2);
		jobInstances.add(jobInstance3);
		Map<String, JobParameter> parametersMap1 = new HashMap<String, JobParameter>();
		parametersMap1.put("param1", new JobParameter("test", true));
		parametersMap1.put("param2", new JobParameter(123l, false));
		JobParameters jobParameters1 = new JobParameters(parametersMap1);
		JobParameters jobParameters2 = new JobParameters(parametersMap1);
		JobExecution jobExecution1 = new JobExecution(jobInstance1, 0l, jobParameters1);
		JobExecution jobExecution2 = new JobExecution(jobInstance2, 3l, jobParameters2);
		Collection<JobExecution> jobExecutions1 = new ArrayList<JobExecution>();
		Collection<JobExecution> jobExecutions2 = new ArrayList<JobExecution>();
		jobExecutions1.add(jobExecution1);
		jobExecutions1.add(jobExecution2);
		jobExecutions2.add(jobExecution2);
		when(jobLocator.getJobNames()).thenReturn(jobNames);
		when(jobService.listJobs(0, 20)).thenReturn(jobNames);
		when(jobService.countJobExecutionsForJob(job1.getName())).thenReturn(2);
		when(jobService.countJobExecutionsForJob(job2.getName())).thenReturn(1);
		// isLaunchable() is always true here.
		when(jobService.isIncrementable(job1.getName())).thenReturn(false);
		when(jobService.isIncrementable(job2.getName())).thenReturn(true);

		when(jobService.listJobInstances(job1.getName(), 0, 20)).thenReturn(jobInstances);
		when(jobService.listJobExecutions(0, 20)).thenReturn(jobExecutions1);
		when(jobService.listJobExecutionsForJob(job2.getName(), 0, 20)).thenReturn(jobExecutions2);
	}

	@Test
	public void testGetBatchJobExecutions() throws Exception {
		mockMvc.perform(
				get("/batch/jobs/executions").param("startJobExecution", "0").param("pageSize", "20").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$", Matchers.hasSize(2)))
				.andExpect(jsonPath("$[*].id", contains(0, 3)))
				.andExpect(jsonPath("$[*].jobId", contains(0, 2)))
				.andExpect(jsonPath("$[*].jobExecution[*].id", contains(0, 3)))
				.andExpect(
						jsonPath("$[*].jobExecution[*].jobParameters.parameters.param1.value", contains("test", "test")))
				.andExpect(
						jsonPath("$[*].jobExecution[*].jobParameters.parameters.param1.type",
								contains("STRING", "STRING")))
				.andExpect(
						jsonPath("$[*].jobExecution[*].jobParameters.parameters.param1.identifying",
								contains(true, true)))
				.andExpect(
						jsonPath("$[*].jobExecution[*].jobParameters.parameters.param2.value", contains(123, 123)))
				.andExpect(
						jsonPath("$[*].jobExecution[*].jobParameters.parameters.param2.type",
								contains("LONG", "LONG")))
				.andExpect(
						jsonPath("$[*].jobExecution[*].jobParameters.parameters.param2.identifying",
								contains(false, false)));
	}

	@Test
	public void testGetJobExecutionsByName() throws Exception {
		mockMvc.perform(
				get("/batch/jobs/job2/executions").param("startJobExecution", "0").param("pageSize", "20").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$", Matchers.hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(3))
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
