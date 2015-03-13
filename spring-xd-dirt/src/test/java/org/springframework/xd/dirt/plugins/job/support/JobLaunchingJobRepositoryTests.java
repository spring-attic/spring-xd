/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.xd.dirt.plugins.job.support;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Michael Minella
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/org/springframework/xd/dirt/plugins/job/support/JobLaunchingJobRepositoryTests-context.xml")
public class JobLaunchingJobRepositoryTests {

	@Autowired
	private JobLaunchingJobRepository jobRepository;

	private JobSupport job = new JobSupport("JobLaunchingJobRepositoryTestsJob");

	private JobParameters jobParameters = new JobParameters();

	@Transactional
	@Test
	public void testGetLastJobExecution() throws Exception {
		JobExecution jobExecution =
				jobRepository.createJobExecution(job.getName(), jobParameters);
		jobExecution.setStatus(BatchStatus.FAILED);
		jobExecution.setEndTime(new Date());
		jobExecution.getExecutionContext().put("foo", "bar");
		jobRepository.update(jobExecution);
		jobRepository.updateExecutionContext(jobExecution);
		Thread.sleep(10);
		jobExecution = jobRepository.createJobExecution(job.getName(), jobParameters);
		StepExecution stepExecution = new StepExecution("step1", jobExecution);
		jobRepository.add(stepExecution);
		jobExecution.addStepExecutions(Arrays.asList(stepExecution));
		JobExecution lastJobExecution =
				jobRepository.getLastJobExecution(job.getName(), jobParameters);
		assertEquals(jobExecution, lastJobExecution);
		assertEquals(stepExecution, jobExecution.getStepExecutions().iterator().next());
		assertEquals(0, lastJobExecution.getExecutionContext().size());
	}


	/*
	 * Save multiple StepExecutions for the same step and check the returned
	 * count and last execution are correct.
	 */
	@Transactional
	@Test
	public void testGetStepExecutionCountAndLastStepExecution() throws Exception {
		job.setRestartable(true);
		StepSupport step = new StepSupport("restartedStep");

		// first execution
		JobExecution firstJobExec =
				jobRepository.createJobExecution(job.getName(), jobParameters);
		StepExecution firstStepExec = new StepExecution(step.getName(), firstJobExec);
		firstStepExec.getExecutionContext().put("foo", "bar");
		jobRepository.add(firstStepExec);

		assertEquals(1, jobRepository.getStepExecutionCount(firstJobExec.getJobInstance(),
															step.getName()));
		StepExecution lastStepExecution =
				jobRepository.getLastStepExecution(firstJobExec.getJobInstance(),
						                           step.getName());
		assertEquals(firstStepExec, lastStepExecution);
		assertEquals(0, lastStepExecution.getExecutionContext().size());

		// first execution failed
		firstJobExec.setStartTime(new Date(4));
		firstStepExec.setStartTime(new Date(5));
		firstStepExec.setStatus(BatchStatus.FAILED);
		firstStepExec.setEndTime(new Date(6));
		jobRepository.update(firstStepExec);
		firstJobExec.setStatus(BatchStatus.FAILED);
		firstJobExec.setEndTime(new Date(7));
		jobRepository.update(firstJobExec);

		// second execution
		JobExecution secondJobExec =
				jobRepository.createJobExecution(job.getName(), jobParameters);
		StepExecution secondStepExec = new StepExecution(step.getName(), secondJobExec);
		secondStepExec.getExecutionContext().put("baz", "qux");
		jobRepository.add(secondStepExec);

		assertEquals(2, jobRepository.getStepExecutionCount(secondJobExec.getJobInstance(),
														    step.getName()));
		StepExecution lastStepExecution1 =
				jobRepository.getLastStepExecution(secondJobExec.getJobInstance(),
						                           step.getName());
		assertEquals(secondStepExec, lastStepExecution1);
		assertEquals(0, lastStepExecution1.getExecutionContext().size());
	}
}
