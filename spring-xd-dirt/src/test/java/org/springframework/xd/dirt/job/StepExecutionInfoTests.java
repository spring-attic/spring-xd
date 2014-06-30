/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.junit.Test;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.support.PartitionStep;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ExecutionContext;

/**
 * @author Michael Minella
 * @author Gunnar Hillert
 */
public class StepExecutionInfoTests {

	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	{
		dateFormat.setTimeZone(TimeZone.getTimeZone("America/Chicago"));
	}

	@Test
	public void testBasicConstructor() {

		StepExecutionInfo info = new StepExecutionInfo("job1", 5l, "step1", TimeZone.getTimeZone("America/Chicago"));

		verify(info, "step1", null, "-", "NONE", 5l, "job1", "-", "NONE", StepType.UNKNOWN.getDisplayName());
		assertEquals(info.getName(), "step1");
		assertNull(info.getId());
		assertEquals(info.getDuration(), "-");
		assertEquals(info.getDurationMillis(), 0);
		assertEquals(info.getExitCode(), "NONE");
		assertEquals((long) info.getJobExecutionId(), 5l);
		assertEquals(info.getJobName(), "job1");
		assertEquals(info.getName(), "step1");
		assertEquals(info.getStartDate(), "-");
		assertEquals(info.getStatus(), "NONE");
		assertNotNull(info.getStepExecution());
		assertEquals(info.getStepType(), StepType.UNKNOWN.getDisplayName());
	}

	@Test
	public void testUnknownTaskletType() {

		JobExecution jobExecution = new JobExecution(5l);
		StepExecution stepExecution = new StepExecution("step1", jobExecution, 3l);
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.put(TaskletStep.TASKLET_TYPE_KEY, this.getClass().getName());
		stepExecution.setExecutionContext(executionContext);

		StepExecutionInfo info = new StepExecutionInfo(stepExecution, TimeZone.getTimeZone("America/Chicago"));

		verify(info, "step1", 3l, "00:00:00", stepExecution.getExitStatus().getExitCode(), 5l, "?",
				dateFormat.format(stepExecution.getStartTime()), stepExecution.getStatus().toString(),
				this.getClass().getName());
	}

	@Test
	public void testKnownTaskletType() {
		JobExecution jobExecution = new JobExecution(5l);
		StepExecution stepExecution = new StepExecution("step1", jobExecution, 3l);
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.put(TaskletStep.TASKLET_TYPE_KEY, TaskletType.CHUNK_ORIENTED_TASKLET.getClassName());
		stepExecution.setExecutionContext(executionContext);

		StepExecutionInfo info = new StepExecutionInfo(stepExecution, TimeZone.getTimeZone("America/Chicago"));

		verify(info, "step1", 3l, "00:00:00", stepExecution.getExitStatus().getExitCode(), 5l, "?",
				dateFormat.format(stepExecution.getStartTime()), stepExecution.getStatus().toString(),
				TaskletType.CHUNK_ORIENTED_TASKLET.getDisplayName());
	}

	@Test
	public void testKnownStepType() {
		JobExecution jobExecution = new JobExecution(5l);
		StepExecution stepExecution = new StepExecution("step1", jobExecution, 3l);
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.put(Step.STEP_TYPE_KEY, PartitionStep.class.getName());
		stepExecution.setExecutionContext(executionContext);

		StepExecutionInfo info = new StepExecutionInfo(stepExecution, TimeZone.getTimeZone("America/Chicago"));

		verify(info, "step1", 3l, "00:00:00", stepExecution.getExitStatus().getExitCode(), 5l, "?",
				dateFormat.format(stepExecution.getStartTime()), stepExecution.getStatus().toString(),
				StepType.PARTITION_STEP.getDisplayName());
	}

	@Test
	public void testUnknownStepType() {
		JobExecution jobExecution = new JobExecution(5l);
		StepExecution stepExecution = new StepExecution("step1", jobExecution, 3l);
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.put(Step.STEP_TYPE_KEY, this.getClass().getName());
		stepExecution.setExecutionContext(executionContext);

		StepExecutionInfo info = new StepExecutionInfo(stepExecution, TimeZone.getTimeZone("America/Chicago"));

		verify(info, "step1", 3l, "00:00:00", stepExecution.getExitStatus().getExitCode(), 5l, "?",
				dateFormat.format(stepExecution.getStartTime()), stepExecution.getStatus().toString(),
				this.getClass().getName());
	}

	private void verify(StepExecutionInfo info, String stepName, Long stepId, String duration, String exitCode,
			long jobExecutionId, String jobName, String startDate, String stepStatus, String stepType) {

		assertEquals(stepName, info.getName());
		assertEquals(stepId, info.getId());
		assertEquals(duration, info.getDuration());
		assertEquals(exitCode, info.getExitCode());
		assertEquals(jobExecutionId, (long) info.getJobExecutionId());
		assertEquals(jobName, info.getJobName());
		assertEquals(startDate, info.getStartDate());
		assertEquals(stepStatus, info.getStatus());
		assertNotNull(info.getStepExecution());
		assertEquals(stepType, info.getStepType());
	}
}
