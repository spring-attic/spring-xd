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

package org.springframework.xd.shell.command;

import java.util.List;
import java.util.Map;

import org.springframework.batch.core.JobParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.xd.rest.client.JobOperations;
import org.springframework.xd.rest.client.domain.JobDefinitionResource;
import org.springframework.xd.rest.client.domain.JobExecutionInfoResource;
import org.springframework.xd.rest.client.domain.StepExecutionInfoResource;
import org.springframework.xd.rest.client.domain.StepExecutionProgressInfoResource;
import org.springframework.xd.shell.XDShell;
import org.springframework.xd.shell.util.CommonUtils;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableHeader;
import org.springframework.xd.shell.util.TableRow;
import org.springframework.xd.shell.util.UiUtils;

/**
 * Job commands.
 * 
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 * 
 */
@Component
public class JobCommands implements CommandMarker {

	private final static String CREATE_JOB = "job create";

	private final static String LIST_JOBS = "job list";

	private final static String LIST_JOB_EXECUTIONS = "job execution list";

	private final static String LIST_STEP_EXECUTIONS = "job execution step list";

	private final static String PROGRESS_STEP_EXECUTION = "job execution step progress";

	private final static String DISPLAY_JOB_EXECUTION = "job execution display";

	private final static String STOP_ALL_JOB_EXECUTIONS = "job execution all stop";

	private final static String STOP_JOB_EXECUTION = "job execution stop";

	private final static String DEPLOY_JOB = "job deploy";

	private final static String DEPLOY_ALL_JOBS = "job all deploy";

	private final static String LAUNCH_JOB = "job launch";

	private final static String UNDEPLOY_JOB = "job undeploy";

	private final static String UNDEPLOY_ALL_JOBS = "job all undeploy";

	private final static String DESTROY_JOB = "job destroy";

	private final static String DESTROY_ALL_JOBS = "job all destroy";

	@Autowired
	private UserInput userInput;

	@Autowired
	private XDShell xdShell;

	@CliAvailabilityIndicator({ CREATE_JOB, LIST_JOBS, DEPLOY_JOB, UNDEPLOY_JOB, DESTROY_JOB, STOP_JOB_EXECUTION })
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = CREATE_JOB, help = "Create a job")
	public String createJob(
			@CliOption(mandatory = true, key = { "name", "" }, help = "the name to give to the job") String name,
			@CliOption(mandatory = true, key = "definition", help = "job definition using xd dsl ") String dsl,
			@CliOption(key = "deploy", help = "whether to deploy the job immediately", unspecifiedDefaultValue = "true") boolean deploy
			) {
		jobOperations().createJob(name, dsl, deploy);
		return String.format((deploy ? "Successfully created and deployed job '%s'"
				: "Successfully created job '%s'"), name);
	}

	@CliCommand(value = LIST_JOBS, help = "List all jobs")
	public Table listJobs() {

		final PagedResources<JobDefinitionResource> jobs = jobOperations().list();
		final Table table = new Table();
		table.addHeader(1, new TableHeader("Job Name")).
				addHeader(2, new TableHeader("Job Definition")).
				addHeader(3, new TableHeader("Status"));

		for (JobDefinitionResource jobDefinitionResource : jobs) {
			final TableRow row = new TableRow();
			row.addValue(1, jobDefinitionResource.getName()).addValue(2, jobDefinitionResource.getDefinition());
			if (Boolean.TRUE.equals(jobDefinitionResource.isDeployed())) {
				row.addValue(3, "deployed");
			}
			else {
				row.addValue(3, "");
			}
			table.getRows().add(row);
		}
		return table;
	}

	@CliCommand(value = LIST_JOB_EXECUTIONS, help = "List all job executions")
	public Table listJobExecutions() {

		final List<JobExecutionInfoResource> jobExecutions = jobOperations().listJobExecutions();
		final Table table = new Table();
		table.addHeader(1, new TableHeader("Id"))
				.addHeader(2, new TableHeader("Job Name"))
				.addHeader(3, new TableHeader("Start Time"))
				.addHeader(4, new TableHeader("Step Execution Count"))
				.addHeader(5, new TableHeader("Status"));

		for (JobExecutionInfoResource jobExecutionInfoResource : jobExecutions) {
			final TableRow row = new TableRow();
			final String startTimeAsString =
					jobExecutionInfoResource.getStartDate() + " " +
							jobExecutionInfoResource.getStartTime() + " " +
							jobExecutionInfoResource.getTimeZone().getID();
			row.addValue(1, String.valueOf(jobExecutionInfoResource.getExecutionId()))
					.addValue(2, jobExecutionInfoResource.getName())
					.addValue(3, startTimeAsString)
					.addValue(4, String.valueOf(jobExecutionInfoResource.getStepExecutionCount()))
					.addValue(5, jobExecutionInfoResource.getJobExecution().getStatus().name());
			table.getRows().add(row);
		}

		return table;
	}

	@CliCommand(value = LIST_STEP_EXECUTIONS, help = "List all step executions for the provided job execution id")
	public Table listStepExecutions(
			@CliOption(mandatory = true, key = { "", "id" }, help = "the id of the job execution") long jobExecutionId) {

		final List<StepExecutionInfoResource> stepExecutions = jobOperations().listStepExecutions(jobExecutionId);
		final Table table = new Table();
		table.addHeader(1, new TableHeader("Id"))
				.addHeader(2, new TableHeader("Step Name"))
				.addHeader(3, new TableHeader("Job Exec ID"))
				.addHeader(4, new TableHeader("Start Time (UTC)"))
				.addHeader(5, new TableHeader("End Time (UTC)"))
				.addHeader(6, new TableHeader("Status"));

		for (StepExecutionInfoResource stepExecutionInfoResource : stepExecutions) {

			final String utcStartTime = CommonUtils.getUtcTime(stepExecutionInfoResource.getStepExecution().getStartTime());
			final String utcEndTime = CommonUtils.getUtcTime(stepExecutionInfoResource.getStepExecution().getEndTime());

			final TableRow row = new TableRow();

			row.addValue(1, String.valueOf(stepExecutionInfoResource.getStepExecution().getId()))
					.addValue(2, stepExecutionInfoResource.getStepExecution().getStepName())
					.addValue(3, String.valueOf(stepExecutionInfoResource.getJobExecutionId()))
					.addValue(4, utcStartTime)
					.addValue(5, utcEndTime)
					.addValue(6, stepExecutionInfoResource.getStepExecution().getStatus().name());
			table.getRows().add(row);
		}

		return table;
	}

	@CliCommand(value = PROGRESS_STEP_EXECUTION, help = "Get the progress info for the given step execution")
	public Table stepExecutionProgress(
			@CliOption(mandatory = true, key = { "", "id" }, help = "the id of the step execution") long stepExecutionId,
			@CliOption(mandatory = true, key = { "jobExecutionId" }, help = "the job execution id") long jobExecutionId) {
		StepExecutionProgressInfoResource progressInfoResource = jobOperations().stepExecutionProgress(jobExecutionId,
				stepExecutionId);
		Table table = new Table();
		table.addHeader(1, new TableHeader("Id"))
				.addHeader(2, new TableHeader("Step Name"))
				.addHeader(3, new TableHeader("Percentage Complete"))
				.addHeader(4, new TableHeader("Duration"));
		final TableRow tableRow = new TableRow();
		tableRow.addValue(1, String.valueOf(progressInfoResource.getStepExecution().getId()))
				.addValue(2, String.valueOf(progressInfoResource.getStepExecution().getStepName()))
				.addValue(3, String.format("%.0f%%", progressInfoResource.getPercentageComplete() * 100))
				.addValue(4, String.format("%.0f ms", progressInfoResource.getDuration()));
		table.getRows().add(tableRow);
		return table;
	}

	@CliCommand(value = DISPLAY_JOB_EXECUTION, help = "Display the details of a Job Execution")
	public String display(
			@CliOption(mandatory = true, key = { "", "id" }, help = "the id of the job execution") long jobExecutionId) {

		final JobExecutionInfoResource jobExecutionInfoResource = jobOperations().displayJobExecution(jobExecutionId);

		final String startTimeAsString =
				jobExecutionInfoResource.getStartDate() + " " +
						jobExecutionInfoResource.getStartTime() + " " +
						jobExecutionInfoResource.getTimeZone().getID();

		final Table jobExecutionTable = new Table();
		jobExecutionTable.addHeader(1, new TableHeader("Property"))
				.addHeader(2, new TableHeader("Value"));

		final StringBuilder details = new StringBuilder();

		details.append("Job Execution Details:\n");
		details.append(UiUtils.HORIZONTAL_LINE);

		final String utcCreateTime = CommonUtils.getUtcTime(jobExecutionInfoResource.getJobExecution().getCreateTime());
		final String utcStartTime = CommonUtils.getUtcTime(jobExecutionInfoResource.getJobExecution().getStartTime());
		final String utcEndTime = CommonUtils.getUtcTime(jobExecutionInfoResource.getJobExecution().getEndTime());

		jobExecutionTable.addRow("Job Execution ID", String.valueOf(jobExecutionInfoResource.getExecutionId()))
				.addRow("Job Name", jobExecutionInfoResource.getName())
				.addRow("Start Time", startTimeAsString)
				.addRow("Create Time (UTC)", utcCreateTime)
				.addRow("Start Time (UTC)", utcStartTime)
				.addRow("End Time (UTC)", utcEndTime)
				.addRow("Running", String.valueOf(jobExecutionInfoResource.getJobExecution().isRunning()))
				.addRow("Stopping", String.valueOf(jobExecutionInfoResource.getJobExecution().isStopping()))
				.addRow("Step Execution Count", String.valueOf(jobExecutionInfoResource.getStepExecutionCount()))
				.addRow("Status", jobExecutionInfoResource.getJobExecution().getStatus().name());

		details.append(jobExecutionTable);

		details.append(UiUtils.HORIZONTAL_LINE);
		details.append("Job Parameters:\n");
		details.append(UiUtils.HORIZONTAL_LINE);

		if (jobExecutionInfoResource.getJobExecution().getJobParameters().isEmpty()) {
			details.append("No Job Parameters are present");
		}
		else {
			final Table jobParameterTable = new Table();
			jobParameterTable.addHeader(1, new TableHeader("Name"))
					.addHeader(2, new TableHeader("Value"))
					.addHeader(3, new TableHeader("Type"))
					.addHeader(4, new TableHeader("Identifying"));

			for (Map.Entry<String, JobParameter> jobParameterEntry : jobExecutionInfoResource.getJobExecution().getJobParameters().getParameters().entrySet()) {

				jobParameterTable.addRow(jobParameterEntry.getKey(),
						jobParameterEntry.getValue().getValue().toString(),
						jobParameterEntry.getValue().getType().name(),
						String.valueOf(jobParameterEntry.getValue().isIdentifying()));
			}

			details.append(jobParameterTable);
		}

		return details.toString();
	}

	@CliCommand(value = STOP_ALL_JOB_EXECUTIONS, help = "Stop all the job executions that are running")
	public String stopAllJobExecutions(
			@CliOption(key = "force", help = "bypass confirmation prompt", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean force) {
		if (force || "y".equalsIgnoreCase(userInput.prompt("Really stop all job executions?", "n", "y", "n"))) {
			jobOperations().stopAllJobExecutions();
			return String.format("Stopped all the job executions");
		}
		else {
			return "";
		}
	}

	@CliCommand(value = STOP_JOB_EXECUTION, help = "Stop the job execution that is running")
	public String stopJobExecution(
			@CliOption(key = { "", "id" }, help = "the id of the job execution", mandatory = true) long executionId) {
		jobOperations().stopJobExecution(executionId);
		return String.format("Stopped Job execution that has executionId '%s'", executionId);
	}

	@CliCommand(value = DEPLOY_JOB, help = "Deploy a previously created job")
	public String deployJob(
			@CliOption(key = { "", "name" }, help = "the name of the job to deploy", mandatory = true, optionContext = "existing-job undeployed disable-string-converter") String name) {
		jobOperations().deploy(name);
		return String.format("Deployed job '%s'", name);
	}

	@CliCommand(value = DEPLOY_ALL_JOBS, help = "Deploy previously created job(s)")
	public String deployAllJobs(
			@CliOption(key = "force", help = "bypass confirmation prompt", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean force
			) {
		if (force || "y".equalsIgnoreCase(userInput.prompt("Really deploy all jobs?", "n", "y", "n"))) {
			jobOperations().deployAll();
			return String.format("Deployed all jobs");
		}
		else {
			return "";
		}
	}

	@CliCommand(value = LAUNCH_JOB, help = "Launch previously deployed job")
	public String launchJob(
			@CliOption(key = { "", "name" }, help = "the name of the job to deploy", optionContext = "existing-job disable-string-converter") String name,
			@CliOption(key = { "params" }, help = "the parameters for the job", unspecifiedDefaultValue = "") String jobParameters) {
		jobOperations().launchJob(name, jobParameters);
		return String.format("Successfully launched the job '%s'", name);
	}

	@CliCommand(value = UNDEPLOY_JOB, help = "Un-deploy an existing job")
	public String undeployJob(
			@CliOption(key = { "", "name" }, help = "the name of the job to un-deploy", mandatory = true, optionContext = "existing-job deployed disable-string-converter") String name
			) {
		jobOperations().undeploy(name);
		return String.format("Un-deployed Job '%s'", name);
	}

	@CliCommand(value = UNDEPLOY_ALL_JOBS, help = "Un-deploy all existing jobs")
	public String undeployAllJobs(
			@CliOption(key = "force", help = "bypass confirmation prompt", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean force
			) {
		if (force || "y".equalsIgnoreCase(userInput.prompt("Really undeploy all jobs?", "n", "y", "n"))) {
			jobOperations().undeployAll();
			return String.format("Un-deployed all the jobs");
		}
		else {
			return "";
		}
	}

	@CliCommand(value = DESTROY_JOB, help = "Destroy an existing job")
	public String destroyJob(
			@CliOption(key = { "", "name" }, help = "the name of the job to destroy", mandatory = true, optionContext = "existing-job disable-string-converter") String name
			) {
		jobOperations().destroy(name);
		return String.format("Destroyed job '%s'", name);
	}

	@CliCommand(value = DESTROY_ALL_JOBS, help = "Destroy all existing jobs")
	public String destroyAllJobs(
			@CliOption(key = "force", help = "bypass confirmation prompt", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean force

			) {
		if (force || "y".equalsIgnoreCase(userInput.prompt("Really destroy all jobs?", "n", "y", "n"))) {
			jobOperations().destroyAll();
			return String.format("Destroyed all the jobs");
		}
		else {
			return "";
		}
	}

	private JobOperations jobOperations() {
		return xdShell.getSpringXDOperations().jobOperations();
	}
}
