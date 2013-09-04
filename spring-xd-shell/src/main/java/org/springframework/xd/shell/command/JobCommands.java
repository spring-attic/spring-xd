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
import org.springframework.xd.shell.util.Assertions;
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

	private final static String UNDEPLOY_JOB = "job undeploy";

	private final static String DESTROY_JOB = "job destroy";

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
			@CliOption(key = "deploy", help = "whether to deploy the stream immediately", unspecifiedDefaultValue = "true") Boolean deploy) {
		jobOperations().createJob(name, dsl, deploy);
		return String.format(((deploy != null && deploy.booleanValue()) ? "Successfully created and deployed job '%s'"
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

	@CliCommand(value = DEPLOY_JOB, help = "Deploy previously created job(s)")
	public String deployJob(
			@CliOption(key = { "", "name" }, help = "the name of the job to deploy", optionContext = "existing-job disable-string-converter") String name,
			@CliOption(key = { "all" }, help = "deploy all the existing jobs", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean all,
			@CliOption(key = "dateFormat", help = "the optional date format for job parameters") String dateFormat,
			@CliOption(key = "numberFormat", help = "the optional number format for job parameters") String numberFormat,
			@CliOption(key = "makeUnique", help = "shall job parameters be made unique?") Boolean makeUnique) {
		String message = "";
		switch (Assertions.exactlyOneOf("name", name, "all", all)) {
			case 0:
				jobOperations().deployJob(name, dateFormat, numberFormat, makeUnique);
				message = String.format("Deployed job '%s'", name);
				break;
			case 1:
				jobOperations().deployAll();
				message = String.format("Deployed all the jobs");
				break;
			default:
				throw new IllegalArgumentException("You must specify exactly one of 'name', 'all'");
		}
		return message;
	}

	@CliCommand(value = UNDEPLOY_JOB, help = "Un-deploy existing job(s)")
	public String undeployJob(
			@CliOption(key = { "", "name" }, help = "the name of the job to un-deploy", optionContext = "existing-job disable-string-converter") String name,
			@CliOption(key = { "all" }, help = "undeploy all the existing jobs", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean all) {
		String message = "";
		switch (Assertions.exactlyOneOf("name", name, "all", all)) {
			case 0:
				jobOperations().undeploy(name);
				message = String.format("Un-deployed Job '%s'", name);
				break;
			case 1:
				jobOperations().undeployAll();
				message = String.format("Un-deployed all the jobs");
				break;
			default:
				throw new IllegalArgumentException("You must specify exactly one of 'name', 'all'");
		}
		return message;
	}

	@CliCommand(value = DESTROY_JOB, help = "Destroy existing job(s)")
	public String destroyJob(
			@CliOption(key = { "", "name" }, help = "the name of the job to destroy", optionContext = "existing-job disable-string-converter") String name,
			@CliOption(key = { "all" }, help = "destroy all the existing jobs", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean all) {
		String message = "";
		switch (Assertions.exactlyOneOf("name", name, "all", all)) {
			case 0:
				jobOperations().destroy(name);
				message = String.format("Destroyed job '%s'", name);
				break;
			case 1:
				jobOperations().destroyAll();
				message = String.format("Destroyed all the jobs");
				break;
			default:
				throw new IllegalArgumentException("You must specify exactly one of 'name', 'all'");
		}
		return message;
	}

	private JobOperations jobOperations() {
		return xdShell.getSpringXDOperations().jobOperations();
	}
}
