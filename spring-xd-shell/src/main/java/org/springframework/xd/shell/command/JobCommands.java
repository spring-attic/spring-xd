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

package org.springframework.xd.shell.command;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.xd.rest.client.JobOperations;
import org.springframework.xd.rest.client.domain.JobDefinitionResource;
import org.springframework.xd.shell.XDShell;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableHeader;
import org.springframework.xd.shell.util.TableRow;
import org.springframework.xd.shell.util.UiUtils;

/**
 * Job commands.
 *
 * @author Glenn Renfro
 */

@Component
public class JobCommands implements CommandMarker {

	private final static String CREATE_JOB = "job create";
	private final static String DEPLOY_JOB = "job deploy";
	private final static String UNDEPLOY_JOB = "job undeploy";
	private final static String DESTROY_JOB = "job destroy";
	private final static String LIST_JOBS = "job list";
	@Autowired
	private XDShell xdShell;

	@CliAvailabilityIndicator({ CREATE_JOB, LIST_JOBS, DEPLOY_JOB, UNDEPLOY_JOB, DESTROY_JOB })
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = CREATE_JOB, help = "Create a job")
	public String createJob(
			@CliOption(mandatory = true, key = "name", help = "the name to give to the job")
			String name,
			@CliOption(mandatory = true, key = { "", "definition" }, help = "job definition using xd dsl ")
			String dsl,
			@CliOption(key = "deploy", help = "whether to deploy the stream immediately", unspecifiedDefaultValue = "true")
			Boolean deploy) {
		try {
			jobOperations().createJob(name,dsl,deploy);
		}
		catch (Exception e) {
			return String.format("Error creating job '%s' \n %s", name,e.getMessage());
		}
		return String.format(((deploy != null && deploy.booleanValue()) ? 
			"Successfully created and deployed job '%s'" : "Successfully created job '%s'"), name);
	}

	@CliCommand(value = DEPLOY_JOB, help = "Deploy a previously created job")
	public String deployJob(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name of the job to deploy")
			String name) {
		jobOperations().deployJob(name);
		return String.format("Deployed job '%s'", name);
	}

	@CliCommand(value = LIST_JOBS, help = "List all jobs")
	public String listJobs() {

		final List<JobDefinitionResource> jobs;

		try {
			jobs = jobOperations().list();
		}
		catch (Exception e) {
			return String.format("Error listing jobs");
		}

		final Table table = new Table();
		table.addHeader(1, new TableHeader("Job Name"))
		     .addHeader(2, new TableHeader("Job Definition"));

		for (JobDefinitionResource jobDefinitionResource : jobs) {
			final TableRow row = new TableRow();
			row.addValue(1, jobDefinitionResource.getName())
			   .addValue(2, jobDefinitionResource.getDefinition());
			table.getRows().add(row);
		}
		return UiUtils.renderTextTable(table);
}

	@CliCommand(value = UNDEPLOY_JOB, help = "Un-deploy a previously deployed job")
	public String undeployStream(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name of the job to un-deploy")
			String name) {
		jobOperations().undeployJob(name);
		return String.format("Un-deployed Job '%s'", name);
	}
	
	@CliCommand(value = DESTROY_JOB, help = "Destroy an existing job")
	public String destroyJob(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name of the job to destroy")
			String name) {
		jobOperations().destroyJob(name);
		return String.format("Destroyed job '%s'", name);
	}
	private JobOperations jobOperations() {
		return xdShell.getSpringXDOperations().jobOperations();
	}
}
