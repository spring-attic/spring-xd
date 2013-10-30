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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
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

	@CliAvailabilityIndicator({ CREATE_JOB, LIST_JOBS, DEPLOY_JOB, UNDEPLOY_JOB, DESTROY_JOB })
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = CREATE_JOB, help = "Create a job")
	public String createJob(
			@CliOption(mandatory = true, key = { "name", "" }, help = "the name to give to the job") String name,
			@CliOption(mandatory = true, key = "definition", help = "job definition using xd dsl ") String dsl,
			@CliOption(key = "deploy", help = "whether to deploy the stream immediately", unspecifiedDefaultValue = "true") boolean deploy,
			@CliOption(key = "dateFormat", help = "the optional date format for job parameters") String dateFormat,
			@CliOption(key = "numberFormat", help = "the optional number format for job parameters") String numberFormat,
			@CliOption(key = "makeUnique", help = "shall job parameters be made unique?", unspecifiedDefaultValue = "false") boolean makeUnique) {
		jobOperations().createJob(name, dsl, dateFormat, numberFormat, makeUnique, deploy);
		return String.format((deploy ? "Successfully created and deployed job '%s'"
				: "Successfully created job '%s'"), name);
	}

	@CliCommand(value = LIST_JOBS, help = "List all jobs")
	public Table listJobs() {

		final PagedResources<JobDefinitionResource> jobs = jobOperations().list();
		final Table table = new Table();
		table.addHeader(1, new TableHeader("Job Name")).addHeader(2, new TableHeader("Job Definition"));

		for (JobDefinitionResource jobDefinitionResource : jobs) {
			final TableRow row = new TableRow();
			row.addValue(1, jobDefinitionResource.getName()).addValue(2, jobDefinitionResource.getDefinition());
			table.getRows().add(row);
		}
		return table;
	}

	@CliCommand(value = DEPLOY_JOB, help = "Deploy a previously created job")
	public String deployJob(
			@CliOption(key = { "", "name" }, help = "the name of the job to deploy", mandatory = true, optionContext = "existing-job disable-string-converter") String name) {
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
			@CliOption(key = { "", "name" }, help = "the name of the job to un-deploy", mandatory = true, optionContext = "existing-job disable-string-converter") String name
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
