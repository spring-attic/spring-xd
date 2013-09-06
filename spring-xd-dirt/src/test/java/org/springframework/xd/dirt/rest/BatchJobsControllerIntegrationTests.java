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
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collection;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.xd.dirt.plugins.job.batch.BatchJobLocator;

/**
 * Tests REST compliance of BatchJobsController endpoints.
 * 
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, Dependencies.class })
public class BatchJobsControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Autowired
	private JobService jobService;

	@Autowired
	private BatchJobLocator jobLocator;

	@Before
	public void before() throws Exception {
		SimpleJob job1 = new SimpleJob("job1.job");
		SimpleJob job2 = new SimpleJob("job2.job");
		Collection<String> jobNames = new ArrayList<String>();
		jobNames.add(job1.getName());
		jobNames.add(job2.getName());
		Collection<JobInstance> jobInstances = new ArrayList<JobInstance>();
		jobInstances.add(new JobInstance(0l, job1.getName()));
		jobInstances.add(new JobInstance(2l, job1.getName()));
		jobInstances.add(new JobInstance(3l, job1.getName()));
		when(jobLocator.getJobNames()).thenReturn(jobNames);
		when(jobService.listJobs(0, 20)).thenReturn(jobNames);
		when(jobService.countJobExecutionsForJob(job1.getName())).thenReturn(2);
		when(jobService.countJobExecutionsForJob(job2.getName())).thenReturn(1);
		// isLaunchable() is always true here.
		when(jobService.isIncrementable(job1.getName())).thenReturn(false);
		when(jobService.isIncrementable(job2.getName())).thenReturn(true);

		when(jobService.listJobInstances(job1.getName(), 0, 20)).thenReturn(jobInstances);
	}

	@Test
	public void testGetBatchJobs() throws Exception {
		mockMvc.perform(
				get("/batch/jobs").param("startJob", "0").param("pageSize", "20").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$", Matchers.hasSize(2)))
				.andExpect(jsonPath("$[*].executionCount", contains(2, 1)))
				.andExpect(jsonPath("$[*].launchable", contains(true, true)))
				.andExpect(jsonPath("$[*].incrementable", contains(false, true)))
				.andExpect(jsonPath("$[0].jobInstanceId", nullValue()))
				.andExpect(jsonPath("$[1].jobInstanceId", nullValue()));
	}

	@Test
	public void testGetJobInstanceByJobName() throws Exception {
		mockMvc.perform(
				get("/batch/jobs/job1/instances").param("startJobInstance", "0").param("pageSize", "20").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$", Matchers.hasSize(3)))
				.andExpect(jsonPath("$[*].id", contains(0, 2, 3)))
				.andExpect(jsonPath("$[*].jobName", contains("job1.job", "job1.job", "job1.job")));
	}

	@Test
	public void testGetJobInfoByJobName() throws Exception {
		mockMvc.perform(
				get("/batch/jobs/job1").param("startJobInstance", "0").param("pageSize", "20").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.executionCount").value(2))
				.andExpect(jsonPath("$.launchable").value(true))
				.andExpect(jsonPath("$.incrementable").value(false))
				.andExpect(jsonPath("$.jobInstanceId", nullValue()));

	}
}
