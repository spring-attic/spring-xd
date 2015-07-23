/*
 * Copyright 2013-2015 the original author or authors.
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.hamcrest.Matchers;
import org.junit.After;
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
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.plugins.job.DistributedJobLocator;
import org.springframework.xd.dirt.stream.JobDefinitionRepository;
import org.springframework.xd.dirt.stream.JobRepository;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.TestModuleDefinitions;
import org.springframework.xd.rest.domain.util.TimeUtils;

/**
 * Tests REST compliance of {@link BatchJobsController} endpoints.
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
	private DistributedJobLocator jobLocator;

	@Autowired
	private BatchJobsController batchJobsController;

	@Autowired
	private JobRepository xdJobRepository;

	private JobExecution execution;

	private TimeZone timeZone = TimeUtils.getDefaultTimeZone();

	private static final String JOB_DEFINITION = "job --cron='*/10 * * * * *'";

	@Autowired
	private ModuleRegistry moduleRegistry;

	@Autowired
	private JobDefinitionRepository jobDefinitionRepository;

	@Before
	public void before() throws Exception {
		ModuleDefinition moduleJobDefinition = TestModuleDefinitions.dummy("job", ModuleType.job);
		ArrayList<ModuleDefinition> moduleDefinitions = new ArrayList<ModuleDefinition>();
		moduleDefinitions.add(moduleJobDefinition);
		when(moduleRegistry.findDefinitions("job")).thenReturn(moduleDefinitions);
		when(moduleRegistry.findDefinition("job", ModuleType.job)).thenReturn(moduleJobDefinition);
		when(jobLocator.getJobNames()).thenReturn(Arrays.asList(new String[] {}));

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

		when(jobService.listJobExecutionsForJob("job1", 0, 1)).thenReturn(jobExecutions);
		when(jobService.listJobExecutions(0, 20)).thenReturn(jobExecutions1);
		when(jobService.listJobExecutionsForJob(job2.getName(), 0, 20)).thenReturn(jobExecutions2);

	}

	@After
	public void cleanUp() {
		jobDefinitionRepository.deleteAll();
		xdJobRepository.deleteAll();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetBatchJobs() throws Exception {
		mockMvc.perform(
				post("/jobs/definitions").param("name", "job1").param("definition", JOB_DEFINITION).accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		mockMvc.perform(
				post("/jobs/definitions").param("name", "job2").param("definition", JOB_DEFINITION).accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		JobExecutionInfo info = new JobExecutionInfo(execution, timeZone);
		mockMvc.perform(
				get("/jobs/configurations").accept(
						MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(jsonPath("$.content", Matchers.hasSize(2))).andExpect(
				jsonPath("$.content[*].executionCount", contains(2, 1))).andExpect(
				jsonPath("$.content[*].launchable", contains(false, true))).andExpect(
				jsonPath("$.content[*].deployed", contains(true, true))).andExpect(
				jsonPath("$.content[*].incrementable", contains(false, true))).andExpect(
				jsonPath("$.content[*].jobInstanceId", contains(nullValue(), nullValue()))).andExpect(
				jsonPath("$.content[*].duration", contains(info.getDuration(), null))).andExpect(
				jsonPath("$.content[*].startTime", contains(info.getStartTime(), null))).andExpect(
				jsonPath("$.content[*].startDate", contains(info.getStartDate(), null))).andExpect(
				jsonPath("$.content[*].stepExecutionCount", contains(info.getStepExecutionCount(), 0))).andExpect(
				jsonPath("$.content[*].jobParameters", contains(info.getJobParametersString(), null)))

				// should contain the display name (ie- without the .job suffix)
				.andExpect(jsonPath("$.content[0].name", equalTo("job1"))).andExpect(
						jsonPath("$.content[0].jobInstanceId", nullValue()))

				.andExpect(jsonPath("$.content[1].name", equalTo("job2"))).andExpect(
						jsonPath("$.content[1].jobInstanceId", nullValue()))

				// exit status is non null for job 0 and null for job 1
				.andExpect(
						jsonPath("$.content[0].exitStatus.exitDescription",
								equalTo(execution.getExitStatus().getExitDescription()))).andExpect(
						jsonPath("$.content[0].exitStatus.exitCode", equalTo(execution.getExitStatus().getExitCode()))).andExpect(
						jsonPath("$.content[0].exitStatus.running", equalTo(false))).andExpect(
						jsonPath("$.content[1].exitStatus", nullValue()));
	}

	@Test
	public void testGetPagedBatchJobs() throws Exception {
		mockMvc.perform(
				post("/jobs/definitions").param("name", "job1").param("definition", JOB_DEFINITION).accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		mockMvc.perform(
				post("/jobs/definitions").param("name", "job2").param("definition", JOB_DEFINITION).accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		JobExecutionInfo info = new JobExecutionInfo(execution, timeZone);
		mockMvc.perform(
				get("/jobs/configurations").param("page", "0").param("size", "1").accept(
						MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(jsonPath("$.content", Matchers.hasSize(1))).andExpect(
				jsonPath("$.content[*].executionCount", contains(2))).andExpect(
				jsonPath("$.content[*].launchable", contains(false))).andExpect(
				jsonPath("$.content[*].deployed", contains(true))).andExpect(
				jsonPath("$.content[*].incrementable", contains(false))).andExpect(
				jsonPath("$.content[*].jobInstanceId", contains(nullValue()))).andExpect(
				jsonPath("$.content[*].duration", contains(info.getDuration()))).andExpect(
				jsonPath("$.content[*].startTime", contains(info.getStartTime()))).andExpect(
				jsonPath("$.content[*].startDate", contains(info.getStartDate()))).andExpect(
				jsonPath("$.content[*].stepExecutionCount", contains(info.getStepExecutionCount()))).andExpect(
				jsonPath("$.content[*].jobParameters", contains(info.getJobParametersString())))

				// should contain the display name (ie- without the .job suffix)
				.andExpect(jsonPath("$.content[0].name", equalTo("job1"))).andExpect(
						jsonPath("$.content[0].jobInstanceId", nullValue()))

				.andExpect(
						jsonPath("$.content[0].exitStatus.exitDescription",
								equalTo(execution.getExitStatus().getExitDescription()))).andExpect(
						jsonPath("$.content[0].exitStatus.exitCode", equalTo(execution.getExitStatus().getExitCode()))).andExpect(
						jsonPath("$.content[0].exitStatus.running", equalTo(false)));
	}

	//XD-3266
	@Test
	public void testGetPagedBatchJobsWithLimit() throws Exception {
		mockMvc.perform(
				post("/jobs/definitions").param("name", "job1").param("definition", JOB_DEFINITION)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());
		mockMvc.perform(
				post("/jobs/definitions").param("name", "job2").param("definition", JOB_DEFINITION)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());
		mockMvc.perform(
				post("/jobs/definitions").param("name", "job3").param("definition", JOB_DEFINITION)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status()
				.isCreated());
		mockMvc.perform(
				get("/jobs/configurations").param("page", "0").param("size", "2")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", Matchers.hasSize(2)))
				.andExpect(jsonPath("$.page.size", equalTo(2)))
				.andExpect(jsonPath("$.page.number", equalTo(0)))
				.andExpect(jsonPath("$.page.totalElements", equalTo(3)))
				.andExpect(jsonPath("$.page.totalPages", equalTo(2)));
		mockMvc.perform(
				get("/jobs/configurations").param("page", "1").param("size", "2")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", Matchers.hasSize(1)))
				.andExpect(jsonPath("$.page.size", equalTo(2)))
				.andExpect(jsonPath("$.page.number", equalTo(1)))
				.andExpect(jsonPath("$.page.totalElements", equalTo(3)))
				.andExpect(jsonPath("$.page.totalPages", equalTo(2)));
	}

	@Test
	public void testGetJobInfoByJobName() throws Exception {
		mockMvc.perform(
				get("/jobs/configurations/job1").param("startJobInstance", "0").param("pageSize", "20").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(
				jsonPath("$.executionCount").value(2)).andExpect(jsonPath("$.launchable").value(false)).andExpect(
				jsonPath("$.incrementable").value(false)).andExpect(jsonPath("$.deployed").value(false)).andExpect(
				jsonPath("$.jobInstanceId", nullValue()));

	}
}
