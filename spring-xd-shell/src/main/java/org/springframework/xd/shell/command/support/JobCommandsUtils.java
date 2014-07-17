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

package org.springframework.xd.shell.command.support;

import java.util.TimeZone;

import org.springframework.batch.core.StepExecution;
import org.springframework.util.StringUtils;
import org.springframework.xd.rest.domain.StepExecutionInfoResource;
import org.springframework.xd.shell.command.JobCommands;
import org.springframework.xd.shell.util.CommonUtils;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableHeader;

/**
 * Provides helper methods for the {@link JobCommands} class, e.g. rendering of textual output.
 *
 * @author Gunnar Hillert
 * @since 1.0
 */
public final class JobCommandsUtils {

	/**
	 * Prevent instantiation.
	 */
	private JobCommandsUtils() {
		throw new AssertionError();
	}

	public static Table prepareStepExecutionTable(final StepExecutionInfoResource stepExecutionInfoResource,
			final TimeZone timeZone) {
		final Table stepExecutionTable = new Table();
		stepExecutionTable.addHeader(1, new TableHeader("Property"))
		.addHeader(2, new TableHeader("Value"));

		String stepId = CommonUtils.NOT_AVAILABLE;
		String jobExecutionIdFromData = CommonUtils.NOT_AVAILABLE;
		String stepName = CommonUtils.NOT_AVAILABLE;
		String startTimeAsString = CommonUtils.NOT_AVAILABLE;
		String endTimeAsString = CommonUtils.NOT_AVAILABLE;
		String durationAsString = CommonUtils.NOT_AVAILABLE;
		String lastUpdatedTimeAsString = CommonUtils.NOT_AVAILABLE;
		String exitStatus = CommonUtils.NOT_AVAILABLE;
		String commitCount = CommonUtils.NOT_AVAILABLE;
		String filterCount = CommonUtils.NOT_AVAILABLE;
		String processSkipCount = CommonUtils.NOT_AVAILABLE;
		String readCount = CommonUtils.NOT_AVAILABLE;
		String readSkipCount = CommonUtils.NOT_AVAILABLE;
		String rollbackCount = CommonUtils.NOT_AVAILABLE;
		String writeCount = CommonUtils.NOT_AVAILABLE;
		String writeSkipCount = CommonUtils.NOT_AVAILABLE;
		String exitDescription = CommonUtils.NOT_AVAILABLE;
		String batchStatus = CommonUtils.NOT_AVAILABLE;

		if (stepExecutionInfoResource.getJobExecutionId() != null) {
			jobExecutionIdFromData = String.valueOf(stepExecutionInfoResource.getJobExecutionId());
		}

		if (stepExecutionInfoResource.getStepExecution() != null) {
			final StepExecution stepExecution = stepExecutionInfoResource.getStepExecution();

			if (stepExecution.getId() != null) {
				stepId = String.valueOf(stepExecution.getId());
			}
			if (stepExecution.getStepName() != null) {
				stepName = stepExecution.getStepName();
			}
			if (stepExecution.getStartTime() != null) {
				startTimeAsString = CommonUtils.getLocalTime(stepExecution.getStartTime(), timeZone);
			}
			if (stepExecution.getEndTime() != null) {
				endTimeAsString = CommonUtils.getLocalTime(stepExecution.getEndTime(), timeZone);
			}
			if (stepExecution.getStartTime() != null && stepExecution.getEndTime() != null) {
				durationAsString = String.format("%s ms",
						stepExecution.getEndTime().getTime() - stepExecution.getStartTime().getTime());
			}

			if (stepExecution.getLastUpdated() != null) {
				lastUpdatedTimeAsString = CommonUtils.getLocalTime(stepExecution.getLastUpdated(), timeZone);
			}
			if (stepExecution.getExitStatus() != null) {
				exitStatus = stepExecution.getExitStatus().getExitCode();
			}

			if (stepExecution.getExitStatus() != null
					&& StringUtils.hasText(stepExecution.getExitStatus().getExitDescription())) {
				exitDescription = stepExecution.getExitStatus().getExitDescription();
			}

			if (stepExecution.getStatus() != null) {
				batchStatus = stepExecution.getStatus().name();
			}

			commitCount = String.valueOf(stepExecution.getCommitCount());
			filterCount = String.valueOf(stepExecution.getFilterCount());
			processSkipCount = String.valueOf(stepExecution.getProcessSkipCount());
			readCount = String.valueOf(stepExecution.getReadCount());
			readSkipCount = String.valueOf(stepExecution.getReadSkipCount());
			rollbackCount = String.valueOf(stepExecution.getRollbackCount());
			writeCount = String.valueOf(stepExecution.getWriteCount());
			writeSkipCount = String.valueOf(stepExecution.getWriteSkipCount());
		}

		stepExecutionTable
		.addRow("Step Execution Id", stepId)
		.addRow("Job Execution Id", jobExecutionIdFromData)
		.addRow("Step Name", stepName)
		.addRow("Start Time", startTimeAsString)
		.addRow("End Time", endTimeAsString)
		.addRow("Duration", durationAsString)
		.addRow("Status", batchStatus)
		.addRow("Last Updated", lastUpdatedTimeAsString)
		.addRow("Read Count", readCount)
		.addRow("Write Count", writeCount)
		.addRow("Filter Count", filterCount)
		.addRow("Read Skip Count", readSkipCount)
		.addRow("Write Skip Count", writeSkipCount)
		.addRow("Process Skip Count", processSkipCount)
		.addRow("Commit Count", commitCount)
		.addRow("Rollback Count", rollbackCount)
		.addRow("Exit Status", exitStatus)
		.addRow("Exit Description", exitDescription);

		return stepExecutionTable;
	}
}
