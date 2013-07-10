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
import org.springframework.hateoas.VndErrors.VndError;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.dirt.stream.JobDefinition;
import org.springframework.xd.dirt.stream.JobDefinitionRepository;
import org.springframework.xd.dirt.stream.JobDeployer;
import org.springframework.xd.dirt.stream.NoSuchStreamException;
import org.springframework.xd.rest.client.domain.JobDefinitionResource;

/**
 * Handles all Job related interactions.
 * 
 * @author Glenn Renfro
 * @since 1.0
 */
@Controller
@RequestMapping("/jobs")
@ExposesResourceFor(JobDefinitionResource.class)
public class JobController {

	private final JobDeployer jobDeployer;

	private final JobDefinitionRepository jobDefinitionRepository;

	private final JobDefinitionResourceAssembler definitionResourceAssembler = new JobDefinitionResourceAssembler();

	@Autowired
	public JobController(JobDeployer streamDeployer,
			JobDefinitionRepository jobDefinitionRepository) {
		this.jobDeployer = streamDeployer;
		this.jobDefinitionRepository = jobDefinitionRepository;
	}

	/**
	 * Create a new Job.
	 * 
	 * @param name
	 *            The name of the job to create (required)
	 * @param definition
	 *            The Job definition, expressed in the XD DSL (required)
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public JobDefinitionResource deploy(@RequestParam("name") String name,
			@RequestParam("definition") String definition) {
		JobDefinition jobDefinition = new JobDefinition(name, definition);
		JobDefinition streamDefinition = jobDeployer.create(jobDefinition);
		jobDeployer.deploy(name);
		JobDefinitionResource result = definitionResourceAssembler
				.toResource(streamDefinition);
		return result;
	}


}
