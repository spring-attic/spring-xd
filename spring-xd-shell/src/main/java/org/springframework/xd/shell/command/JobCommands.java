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

package org.springframework.xd.shell.command;

import static org.springframework.xd.shell.command.DeploymentOptionKeys.PROPERTIES_FILE_OPTION;
import static org.springframework.xd.shell.command.DeploymentOptionKeys.PROPERTIES_OPTION;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.batch.core.JobParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.xd.rest.client.JobOperations;
import org.springframework.xd.rest.domain.JobDefinitionResource;
import org.springframework.xd.rest.domain.JobExecutionInfoResource;
import org.springframework.xd.rest.domain.JobInstanceInfoResource;
import org.springframework.xd.rest.domain.StepExecutionInfoResource;
import org.springframework.xd.rest.domain.StepExecutionProgressInfoResource;
import org.springframework.xd.rest.domain.support.DeploymentPropertiesFormat;
import org.springframework.xd.shell.Configuration;
import org.springframework.xd.shell.XDShell;
import org.springframework.xd.shell.command.support.JobCommandsUtils;
import org.springframework.xd.shell.util.Assertions;
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
 * @author Eric Bottard
 */
@Component
public class JobCommands implements CommandMarker {

	private final static String CREATE_JOB = "job create";

	private final static String LIST_JOBS = "job list";

	private final static String LIST_JOB_EXECUTIONS = "job execution list";

	private final static String LIST_STEP_EXECUTIONS = "job execution step list";

	private final static String PROGRESS_STEP_EXECUTION = "job execution step progress";

	private final static String DISPLAY_STEP_EXECUTION = "job execution step display";

	private final static String DISPLAY_JOB_EXECUTION = "job execution display";

	private final static String DISPLAY_JOB_INSTANCE = "job instance display";

	private final static String STOP_ALL_JOB_EXECUTIONS = "job execution all stop";

	private final static String STOP_JOB_EXECUTION = "job execution stop";

	private final static String RESTART_JOB_EXECUTION = "job execution restart";

	private final static String DEPLOY_JOB = "job deploy";

	private final static String LAUNCH_JOB = "job launch";

	private final static String UNDEPLOY_JOB = "job undeploy";

	private final static String UNDEPLOY_ALL_JOBS = "job all undeploy";

	private final static String DESTROY_JOB = "job destroy";

	private final static String DESTROY_ALL_JOBS = "job all destroy";

	@Autowired
	private UserInput userInput;

	@Autowired
	private Configuration configuration;

	@Autowired
	private XDShell xdShell;

	@CliAvailabilityIndicator({ CREATE_JOB, LIST_JOBS, DEPLOY_JOB, UNDEPLOY_JOB, DESTROY_JOB, STOP_JOB_EXECUTION,
		DESTROY_ALL_JOBS, UNDEPLOY_ALL_JOBS, STOP_ALL_JOB_EXECUTIONS, DISPLAY_JOB_EXECUTION, LIST_JOB_EXECUTIONS,
		RESTART_JOB_EXECUTION, DISPLAY_STEP_EXECUTION, LIST_STEP_EXECUTIONS, PROGRESS_STEP_EXECUTION,
		DISPLAY_JOB_INSTANCE, LAUNCH_JOB })
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = CREATE_JOB, help = "Create a job")
	public String createJob(
			@CliOption(mandatory = true, key = { "name", "" }, help = "the name to give to the job") String name,
			@CliOption(mandatory = true, key = "definition", optionContext = "completion-job disable-string-converter", help = "job definition using xd dsl ") String dsl,
			@CliOption(key = "deploy", help = "whether to deploy the job immediately", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean deploy
			) {
		jobOperations().createJob(name, dsl, deploy);
		return String.format((deploy ? "Successfully created and deployed job '%s'"
				: "Successfully created job '%s'"), name);
	}

	@CliCommand(value = LIST_JOBS, help = "List all jobs")
	public Table listJobs() {

		final PagedResources<JobDefinitionResource> jobs = jobOperations().list();
		final Table table = new Table()
				.addHeader(1, new TableHeader("Job Name"))
				.addHeader(2, new TableHeader("Job Definition"))
				.addHeader(3, new TableHeader("Status"));

		for (JobDefinitionResource jobDefinitionResource : jobs) {
			table.newRow()
					.addValue(1, jobDefinitionResource.getName())
					.addValue(2, jobDefinitionResource.getDefinition())
					.addValue(3, jobDefinitionResource.getStatus());
		}
		return table;
	}

	@CliCommand(value = LIST_JOB_EXECUTIONS, help = "List all job executions")
	public Table listJobExecutions() {
		final PagedResources<JobExecutionInfoResource> jobExecutions = jobOperations().listJobExecutions();
		return createJobExecutionsTable(jobExecutions.getContent());
	}

	private Table createJobExecutionsTable(Collection<JobExecutionInfoResource> jobExecutions) {

		final Table table = new Table();
		table.addHeader(1, new TableHeader("Id"))
				.addHeader(2, new TableHeader("Job Name"))
				.addHeader(3, new TableHeader("Start Time"))
				.addHeader(4, new TableHeader("Step Execution Count"))
				.addHeader(5, new TableHeader("Execution Status"))
				.addHeader(6, new TableHeader("Deployment Status"))
				.addHeader(7, new TableHeader("Definition Status"))
				.addHeader(8, new TableHeader("Composed"));

		for (JobExecutionInfoResource jobExecutionInfoResource : jobExecutions) {
			final TableRow row = new TableRow();
			final String startTimeAsString = this.configuration.getLocalTime(jobExecutionInfoResource.getJobExecution().getStartTime());

			row.addValue(1, String.valueOf(jobExecutionInfoResource.getExecutionId()))
					.addValue(2, jobExecutionInfoResource.getName())
					.addValue(3, startTimeAsString)
					.addValue(4, String.valueOf(jobExecutionInfoResource.getStepExecutionCount()))
					.addValue(5, jobExecutionInfoResource.getJobExecution().getStatus().name())
					.addValue(6, (jobExecutionInfoResource.isDeployed()) ? "Deployed" : "Undeployed")
					.addValue(7, (jobExecutionInfoResource.isDeleted()) ? "Destroyed" : "Exists")
					.addValue(8, (jobExecutionInfoResource.isComposedJob()) ? "   *" : "");
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
				.addHeader(4, new TableHeader("Start Time"))
				.addHeader(5, new TableHeader("End Time"))
				.addHeader(6, new TableHeader("Status"));

		for (StepExecutionInfoResource stepExecutionInfoResource : stepExecutions) {

			final String localStartTime = this.configuration.getLocalTime(stepExecutionInfoResource.getStepExecution().getStartTime());
			final Date endTimeDate = stepExecutionInfoResource.getStepExecution().getEndTime();
			final String localEndTime = (endTimeDate == null) ? "" : this.configuration.getLocalTime(endTimeDate);
			final TableRow row = new TableRow();

			row.addValue(1, String.valueOf(stepExecutionInfoResource.getStepExecution().getId()))
					.addValue(2, stepExecutionInfoResource.getStepExecution().getStepName())
					.addValue(3, String.valueOf(stepExecutionInfoResource.getJobExecutionId()))
					.addValue(4, localStartTime)
					.addValue(5, localEndTime)
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

	@CliCommand(value = DISPLAY_STEP_EXECUTION, help = "Display the details of a Step Execution")
	public Table displayStepExecution(
			@CliOption(mandatory = true, key = { "", "id" }, help = "the id of the step execution") long stepExecutionId,
			@CliOption(mandatory = true, key = { "jobExecutionId" }, help = "the job execution id") long jobExecutionId) {
		final StepExecutionInfoResource stepExecutionInfoResource = jobOperations().displayStepExecution(
				jobExecutionId,
				stepExecutionId);

		final Table stepExecutionTable = JobCommandsUtils.prepareStepExecutionTable(stepExecutionInfoResource,
				this.configuration.getClientTimeZone());

		return stepExecutionTable;
	}

	@CliCommand(value = DISPLAY_JOB_EXECUTION, help = "Display the details of a Job Execution")
	public String display(
			@CliOption(mandatory = true, key = { "", "id" }, help = "the id of the job execution") long jobExecutionId) {

		final JobExecutionInfoResource jobExecutionInfoResource = jobOperations().displayJobExecution(jobExecutionId);

		final Table jobExecutionTable = new Table();
		jobExecutionTable.addHeader(1, new TableHeader("Property"))
				.addHeader(2, new TableHeader("Value"));

		final StringBuilder details = new StringBuilder();

		details.append("Job Execution Details:\n");
		details.append(UiUtils.HORIZONTAL_LINE);

		final String localCreateTime = this.configuration.getLocalTime(jobExecutionInfoResource.getJobExecution().getCreateTime());
		final String localStartTime = this.configuration.getLocalTime(jobExecutionInfoResource.getJobExecution().getStartTime());
		final Date endTimeDate = jobExecutionInfoResource.getJobExecution().getEndTime();
		final String localEndTime = (endTimeDate == null) ? "" : this.configuration.getLocalTime(endTimeDate);

		jobExecutionTable.addRow("Job Execution ID", String.valueOf(jobExecutionInfoResource.getExecutionId()))
				.addRow("Job Name", jobExecutionInfoResource.getName())
				.addRow("Job Instance", String.valueOf(jobExecutionInfoResource.getJobExecution().getJobInstance().getId()))
				.addRow("Composed Job", String.valueOf(jobExecutionInfoResource.isComposedJob()))
				.addRow("Create Time", localCreateTime)
				.addRow("Start Time", localStartTime)
				.addRow("End Time", localEndTime)
				.addRow("Running", String.valueOf(jobExecutionInfoResource.getJobExecution().isRunning()))
				.addRow("Stopping", String.valueOf(jobExecutionInfoResource.getJobExecution().isStopping()))
				.addRow("Step Execution Count", String.valueOf(jobExecutionInfoResource.getStepExecutionCount()))
				.addRow("Execution Status", jobExecutionInfoResource.getJobExecution().getStatus().name());

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

		final String jobDefinitionStatus;
		if (jobExecutionInfoResource.isDeleted()) {
			jobDefinitionStatus = "Deleted";
		}
		else if (!jobExecutionInfoResource.isDeleted() && !jobExecutionInfoResource.isDeployed()) {
			jobDefinitionStatus = "Undeployed";
		}
		else {
			jobDefinitionStatus = "Deployed";
		}

		details.append(UiUtils.HORIZONTAL_LINE);
		details.append("Underlying Job Definition Status: " + jobDefinitionStatus + "\n");
		details.append(UiUtils.HORIZONTAL_LINE);

		if (!jobExecutionInfoResource.getChildJobExecutions().isEmpty()) {
			details.append("Composed Job - Child Job Executions:\n");
			details.append(UiUtils.HORIZONTAL_LINE);
			details.append(createJobExecutionsTable(jobExecutionInfoResource.getChildJobExecutions()));
		}
		
		return details.toString();
	}

	@CliCommand(value = STOP_ALL_JOB_EXECUTIONS, help = "Stop all the job executions that are running")
	public String stopAllJobExecutions(
			@CliOption(key = "force", help = "bypass confirmation prompt", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean force) {
		if (force || "y".equalsIgnoreCase(userInput.promptWithOptions("Really stop all job executions?", "n", "y", "n"))) {
			jobOperations().stopAllJobExecutions();
			return String.format("Stopped all the job executions");
		}
		else {
			return "";
		}
	}

	@CliCommand(value = STOP_JOB_EXECUTION, help = "Stop a job execution that is running")
	public String stopJobExecution(
			@CliOption(key = { "", "id" }, help = "the id of the job execution", mandatory = true) long executionId) {
		jobOperations().stopJobExecution(executionId);
		return String.format("Stopped Job execution that has executionId '%s'", executionId);
	}

	@CliCommand(value = RESTART_JOB_EXECUTION, help = "Restart a job that failed or interrupted previously")
	public String restartJobExecution(
			@CliOption(key = { "", "id" }, help = "the id of the job execution that failed or interrupted", mandatory = true) long executionId) {
		jobOperations().restartJobExecution(executionId);
		return String.format("Restarted Job execution that had executionId '%s'", executionId);
	}

	@CliCommand(value = DEPLOY_JOB, help = "Deploy a previously created job")
	public String deployJob(
			@CliOption(key = { "", "name" }, help = "the name of the job to deploy", mandatory = true, optionContext = "existing-job undeployed disable-string-converter") String name,
			@CliOption(key = { PROPERTIES_OPTION }, help = "the properties for this deployment", mandatory = false) String properties,
			@CliOption(key = { PROPERTIES_FILE_OPTION }, help = "the properties for this deployment (as a File)", mandatory = false) File propertiesFile
			) throws IOException {

		int which = Assertions.atMostOneOf(PROPERTIES_OPTION, properties, PROPERTIES_FILE_OPTION, propertiesFile);
		Map<String, String> propertiesToUse;
		switch (which) {
			case 0:
				propertiesToUse = DeploymentPropertiesFormat.parseDeploymentProperties(properties);
				break;
			case 1:
				Properties props = new Properties();
				try (FileInputStream fis = new FileInputStream(propertiesFile)) {
					props.load(fis);
				}
				propertiesToUse = DeploymentPropertiesFormat.convert(props);
				break;
			case -1: // Neither option specified
				propertiesToUse = Collections.<String, String> emptyMap();
				break;
			default:
				throw new AssertionError();
		}

		jobOperations().deploy(name, propertiesToUse);
		return String.format("Deployed job '%s'", name);
	}

	@CliCommand(value = LAUNCH_JOB, help = "Launch previously deployed job")
	public String launchJob(
			@CliOption(key = { "", "name" }, help = "the name of the job to deploy", optionContext = "existing-job disable-string-converter") String name,
			@CliOption(key = { "params" }, help = "the parameters for the job", unspecifiedDefaultValue = "") String jobParameters) {
		jobOperations().launchJob(name, jobParameters);
		return String.format("Successfully submitted launch request for job '%s'", name);
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
		if (force || "y".equalsIgnoreCase(userInput.promptWithOptions("Really undeploy all jobs?", "n", "y", "n"))) {
			jobOperations().undeployAll();
			return String.format("Un-deployed all the jobs");
		}
		else {
			return "";
		}
	}

	@CliCommand(value = DISPLAY_JOB_INSTANCE, help = "Display information about a given job instance")
	public CharSequence displayJobInstance(
			@CliOption(key = { "", "id" }, help = "the id of the job instance to retrieve") long instanceId
			) {
		JobInstanceInfoResource jobInstance = jobOperations().displayJobInstance(instanceId);
		StringBuilder result = new StringBuilder("Information about instance ");
		result.append(jobInstance.getInstanceId()).append(" of the job '").append(jobInstance.getJobName()).append(
				"':\n");
		Table table = new Table();
		table.addHeader(1, new TableHeader("Name"))
				.addHeader(2, new TableHeader("Execution Id"))
				.addHeader(3, new TableHeader("Start Time"))
				.addHeader(4, new TableHeader("Step Execution Count"))
				.addHeader(5, new TableHeader("Status"))
				.addHeader(6, new TableHeader("Job Parameters"));

		for (JobExecutionInfoResource jobExecutionInfoResource : jobInstance.getJobExecutions()) {
			String startTimeAsString =
					jobExecutionInfoResource.getStartDate() + " " +
							jobExecutionInfoResource.getStartTime() + " " +
							jobExecutionInfoResource.getTimeZone().getID();
			table.addRow(//
					jobInstance.getJobName(), //
					String.valueOf(jobExecutionInfoResource.getExecutionId()), //
					startTimeAsString, //
					String.valueOf(jobExecutionInfoResource.getStepExecutionCount()), //
					jobExecutionInfoResource.getJobExecution().getStatus().name(), //
					jobExecutionInfoResource.getJobParametersString()//
			);
		}
		result.append(table);
		return result;
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
		if (force || "y".equalsIgnoreCase(userInput.promptWithOptions("Really destroy all jobs?", "n", "y", "n"))) {
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
