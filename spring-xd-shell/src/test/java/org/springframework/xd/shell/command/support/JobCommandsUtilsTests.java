/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.shell.command.support;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.junit.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.xd.dirt.job.StepType;
import org.springframework.xd.rest.domain.StepExecutionInfoResource;
import org.springframework.xd.shell.util.Table;


/**
 * @author Gunnar Hillert
 * @since 1.0
 */
public class JobCommandsUtilsTests {

	@Test
	public void testPrepareStepExecutionTable() {

		final JobExecution jobExecution = new JobExecution(1L, null, "hello");
		jobExecution.getJobConfigurationName();
		final StepExecution stepExecution = new StepExecution("coolStep", jobExecution, 333L);

		final Date startTime = new DateTime(2014, 05, 01, 15, 0).toDate();
		final Date endTime = new DateTime(2014, 05, 01, 15, 1).toDate();
		final Date lastUpdated = new DateTime(2014, 05, 05, 1, 0).toDate();
		stepExecution.setStartTime(startTime);
		stepExecution.setEndTime(endTime);
		stepExecution.setLastUpdated(lastUpdated);
		stepExecution.setStatus(BatchStatus.COMPLETED);
		stepExecution.setExitStatus(ExitStatus.COMPLETED.addExitDescription("We are done."));
		final StepExecutionInfoResource stepExecutionInfoResource = new StepExecutionInfoResource(444L, stepExecution,
				StepType.TASKLET_STEP.getDisplayName());

		final Table table = JobCommandsUtils.prepareStepExecutionTable(stepExecutionInfoResource, TimeZone.getDefault());

		assertEquals("Step Execution Id", table.getRows().get(0).getValue(1));
		assertEquals("333", table.getRows().get(0).getValue(2));

		assertEquals("Job Execution Id", table.getRows().get(1).getValue(1));
		assertEquals("444", table.getRows().get(1).getValue(2));

		assertEquals("Step Name", table.getRows().get(2).getValue(1));
		assertEquals("coolStep", table.getRows().get(2).getValue(2));

		assertEquals("Start Time", table.getRows().get(3).getValue(1));
		assertEquals("2014-05-01 15:00:00,000", table.getRows().get(3).getValue(2));

		assertEquals("End Time", table.getRows().get(4).getValue(1));
		assertEquals("2014-05-01 15:01:00,000", table.getRows().get(4).getValue(2));

		assertEquals("Duration", table.getRows().get(5).getValue(1));
		assertEquals("60000 ms", table.getRows().get(5).getValue(2));

		assertEquals("Status", table.getRows().get(6).getValue(1));
		assertEquals("COMPLETED", table.getRows().get(6).getValue(2));

		assertEquals("Last Updated", table.getRows().get(7).getValue(1));
		assertEquals("2014-05-05 01:00:00,000", table.getRows().get(7).getValue(2));

		assertEquals("Read Count", table.getRows().get(8).getValue(1));
		assertEquals("0", table.getRows().get(8).getValue(2));
		assertEquals("Write Count", table.getRows().get(9).getValue(1));
		assertEquals("0", table.getRows().get(9).getValue(2));
		assertEquals("Filter Count", table.getRows().get(10).getValue(1));
		assertEquals("0", table.getRows().get(10).getValue(2));
		assertEquals("Read Skip Count", table.getRows().get(11).getValue(1));
		assertEquals("0", table.getRows().get(11).getValue(2));
		assertEquals("Write Skip Count", table.getRows().get(12).getValue(1));
		assertEquals("0", table.getRows().get(12).getValue(2));
		assertEquals("Process Skip Count", table.getRows().get(13).getValue(1));
		assertEquals("0", table.getRows().get(13).getValue(2));
		assertEquals("Commit Count", table.getRows().get(14).getValue(1));
		assertEquals("0", table.getRows().get(14).getValue(2));

		assertEquals("Rollback Count", table.getRows().get(15).getValue(1));
		assertEquals("0", table.getRows().get(15).getValue(2));

		assertEquals("Exit Status", table.getRows().get(16).getValue(1));
		assertEquals("COMPLETED", table.getRows().get(16).getValue(2));

		assertEquals("Exit Description", table.getRows().get(17).getValue(1));
		assertEquals("We are done.", table.getRows().get(17).getValue(2));
	}

	@Test
	public void testPrepareStepExecutionTableWithNullStepExecution() {

		final StepExecutionInfoResource stepExecutionInfoResource = new StepExecutionInfoResource();

		final Table table = JobCommandsUtils.prepareStepExecutionTable(stepExecutionInfoResource, TimeZone.getDefault());

		assertEquals("Step Execution Id", table.getRows().get(0).getValue(1));
		assertEquals("N/A", table.getRows().get(0).getValue(2));

		assertEquals("Job Execution Id", table.getRows().get(1).getValue(1));
		assertEquals("N/A", table.getRows().get(1).getValue(2));

		assertEquals("Step Name", table.getRows().get(2).getValue(1));
		assertEquals("N/A", table.getRows().get(2).getValue(2));

		assertEquals("Start Time", table.getRows().get(3).getValue(1));
		assertEquals("N/A", table.getRows().get(3).getValue(2));

		assertEquals("End Time", table.getRows().get(4).getValue(1));
		assertEquals("N/A", table.getRows().get(4).getValue(2));

		assertEquals("Duration", table.getRows().get(5).getValue(1));
		assertEquals("N/A", table.getRows().get(5).getValue(2));

		assertEquals("Status", table.getRows().get(6).getValue(1));
		assertEquals("N/A", table.getRows().get(6).getValue(2));

		assertEquals("Last Updated", table.getRows().get(7).getValue(1));
		assertEquals("N/A", table.getRows().get(7).getValue(2));

		assertEquals("Read Count", table.getRows().get(8).getValue(1));
		assertEquals("N/A", table.getRows().get(8).getValue(2));
		assertEquals("Write Count", table.getRows().get(9).getValue(1));
		assertEquals("N/A", table.getRows().get(9).getValue(2));
		assertEquals("Filter Count", table.getRows().get(10).getValue(1));
		assertEquals("N/A", table.getRows().get(10).getValue(2));
		assertEquals("Read Skip Count", table.getRows().get(11).getValue(1));
		assertEquals("N/A", table.getRows().get(11).getValue(2));
		assertEquals("Write Skip Count", table.getRows().get(12).getValue(1));
		assertEquals("N/A", table.getRows().get(12).getValue(2));
		assertEquals("Process Skip Count", table.getRows().get(13).getValue(1));
		assertEquals("N/A", table.getRows().get(13).getValue(2));
		assertEquals("Commit Count", table.getRows().get(14).getValue(1));
		assertEquals("N/A", table.getRows().get(14).getValue(2));

		assertEquals("Rollback Count", table.getRows().get(15).getValue(1));
		assertEquals("N/A", table.getRows().get(15).getValue(2));

		assertEquals("Exit Status", table.getRows().get(16).getValue(1));
		assertEquals("N/A", table.getRows().get(16).getValue(2));

		assertEquals("Exit Description", table.getRows().get(17).getValue(1));
		assertEquals("N/A", table.getRows().get(17).getValue(2));
	}

	@Test
	public void testPrepareStepExecutionTableWithoutExitDescription() {

		final JobExecution jobExecution = new JobExecution(1L, null, "hello");
		jobExecution.getJobConfigurationName();
		final StepExecution stepExecution = new StepExecution("coolStep", jobExecution, 333L);

		final Date startTime = new DateTime(2014, 05, 01, 15, 0).toDate();
		final Date endTime = new DateTime(2014, 05, 01, 15, 1).toDate();
		final Date lastUpdated = new DateTime(2014, 05, 05, 1, 0).toDate();
		stepExecution.setStartTime(startTime);
		stepExecution.setEndTime(endTime);
		stepExecution.setLastUpdated(lastUpdated);
		stepExecution.setStatus(BatchStatus.COMPLETED);
		stepExecution.setExitStatus(ExitStatus.COMPLETED);
		final StepExecutionInfoResource stepExecutionInfoResource = new StepExecutionInfoResource(444L, stepExecution,
				StepType.TASKLET_STEP.getDisplayName());

		final Table table = JobCommandsUtils.prepareStepExecutionTable(stepExecutionInfoResource, TimeZone.getDefault());

		assertEquals("Exit Description", table.getRows().get(17).getValue(1));
		assertEquals("N/A", table.getRows().get(17).getValue(2));
	}
}
