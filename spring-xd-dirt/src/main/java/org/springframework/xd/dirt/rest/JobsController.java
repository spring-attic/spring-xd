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

package org.springframework.xd.dirt.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.dirt.stream.JobDefinition;
import org.springframework.xd.dirt.stream.JobDefinitionRepository;
import org.springframework.xd.dirt.stream.JobDeployer;
import org.springframework.xd.dirt.stream.NoSuchDefinitionException;
import org.springframework.xd.rest.client.domain.JobDefinitionResource;

/**
 * Handles all Job related interactions.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @since 1.0
 */
@Controller
@RequestMapping("/jobs")
@ExposesResourceFor(JobDefinitionResource.class)
public class JobsController {

	private final JobDeployer jobDeployer;

	private final JobDefinitionResourceAssembler definitionResourceAssembler = new JobDefinitionResourceAssembler();

	@Autowired
	public JobsController(JobDeployer streamDeployer, JobDefinitionRepository jobDefinitionRepository) {
		this.jobDeployer = streamDeployer;
	}

	/**
	 * Request removal of an existing job.
	 *
	 * @param name the name of an existing job (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void delete(@PathVariable("name") String name) {
		jobDeployer.delete(name);
	}

	/**
	 * Request deployment of an existing named job.
	 *
	 * @param name the name of an existing job (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.PUT, params = "deploy=true")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void deploy(@PathVariable("name") String name) {
		jobDeployer.deploy(name);
	}

	/**
	 * Retrieve information about a single {@link JobDefinition}.
	 *
	 * @param name the name of an existing tap (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public JobDefinitionResource display(@PathVariable("name") String name) {
		final JobDefinition jobDefinition = jobDeployer.findOne(name);
		if (jobDefinition == null) {
			throw new NoSuchDefinitionException(name, "There is no job definition named '%s'");
		}
		return definitionResourceAssembler.toResource(jobDefinition);
	}

	/**
	 * List job definitions.
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Iterable<JobDefinitionResource> list() {
		return  definitionResourceAssembler.toResources(jobDeployer.findAll());
	}

	/**
	 * Create a new Job.
	 *
	 * @param name The name of the job to create (required)
	 * @param definition The Job definition, expressed in the XD DSL (required)
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public JobDefinitionResource save(@RequestParam("name") String name,
			@RequestParam("definition") String definition,
			@RequestParam(value = "deploy", defaultValue = "true") boolean deploy) {
		final JobDefinition jobDefinition = new JobDefinition(name, definition);
		final JobDefinition savedJobDefinition = jobDeployer.create(jobDefinition);
		if(deploy) {
			jobDeployer.deploy(name);
		}
		final JobDefinitionResource result = definitionResourceAssembler.toResource(savedJobDefinition);
		return result;
	}

	/**
	 * Request un-deployment of an existing named job.
	 *
	 * @param name the name of an existing job (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.PUT, params = "deploy=false")
	@ResponseStatus(HttpStatus.OK)
	public void undeploy(@PathVariable("name") String name) {
		jobDeployer.undeployJob(name);
	}

}
