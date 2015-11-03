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

package org.springframework.xd.dirt.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.dirt.job.BatchJobAlreadyExistsException;
import org.springframework.xd.dirt.module.support.ModuleDefinitionService;
import org.springframework.xd.dirt.job.dsl.ComposedJobUtil;
import org.springframework.xd.dirt.plugins.job.DistributedJobLocator;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentUnitType;
import org.springframework.xd.dirt.stream.Job;
import org.springframework.xd.dirt.stream.JobDefinition;
import org.springframework.xd.dirt.stream.JobDeployer;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.rest.domain.JobDefinitionResource;

/**
 * Handles all Job related interactions.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author David Turanski
 */
@Controller
@RequestMapping("/jobs")
@ExposesResourceFor(JobDefinitionResource.class)
public class JobsController extends
		XDController<JobDefinition, JobDefinitionResourceAssembler, JobDefinitionResource, Job> {

	private ModuleDefinitionService moduleDefinitionService;
	
	@Autowired
	private DistributedJobLocator distributedJobLocator;

	@Autowired
	public JobsController(JobDeployer jobDeployer, ModuleDefinitionService moduleDefinitionService) {
		super(jobDeployer, new JobDefinitionResourceAssembler(), DeploymentUnitType.Job);
		this.moduleDefinitionService = moduleDefinitionService;
	}

	@Override
	@RequestMapping(value = "/definitions", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void save(@RequestParam("name") String name, @RequestParam("definition") String definition,
			@RequestParam(value = "deploy", defaultValue = "true") boolean deploy) throws Exception {
		// Verify if the batch job repository already has the job with the same name.
		if (distributedJobLocator.getJobNames().contains(name)) {
			throw new BatchJobAlreadyExistsException(name);
		}
		if(ComposedJobUtil.isComposedJobDefinition(definition)) {
			moduleDefinitionService.compose(ComposedJobUtil.getComposedJobModuleName(name), ModuleType.job, definition, false);
		}
		else {
			ComposedJobUtil.validateNotSingleJobInstance(definition, distributedJobLocator.getJobNames());
		}
		super.save(name, definition, deploy);
	}


	/**
	 * Request removal of an existing resource definition (stream or job).
	 *
	 * @param name the name of an existing definition (required)
	 */
	@RequestMapping(value = "/definitions/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void delete(@PathVariable("name") String name) throws Exception {
		super.delete(name);
		if( moduleDefinitionService.findDefinition(
				ComposedJobUtil.getComposedJobModuleName(name), ModuleType.job) != null) {
			moduleDefinitionService.delete(ComposedJobUtil.getComposedJobModuleName(name), ModuleType.job);
		}
	}

	/**
	 * Request removal of all definitions.
	 */
	@RequestMapping(value = "/definitions", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void deleteAll() throws Exception {
		Iterable<JobDefinition> jobDefinitions = deployer.findAll();
		super.deleteAll();
		for(JobDefinition jobDefinition : jobDefinitions){
			if( moduleDefinitionService.findDefinition(
					ComposedJobUtil.getComposedJobModuleName(jobDefinition.getName()), ModuleType.job) != null) {
				moduleDefinitionService.delete(ComposedJobUtil.getComposedJobModuleName(jobDefinition.getName()), ModuleType.job);
			}
		}
	}

	/**
	 * List job definitions.
	 */
	@RequestMapping(value = "/definitions", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public PagedResources<JobDefinitionResource> list(Pageable pageable,
			PagedResourcesAssembler<JobDefinition> assembler) {

		PagedResources<JobDefinitionResource> pagedResources = listValues(pageable, assembler);

		final List<JobDefinitionResource> maskedContents = new ArrayList<JobDefinitionResource>(
				pagedResources.getContent().size());

		for (JobDefinitionResource jobDefinitionResource : pagedResources.getContent()) {
			jobDefinitionResource.getDefinition();
			JobDefinitionResource maskedJobDefinitionResource =
					new JobDefinitionResource(jobDefinitionResource.getName(),
							PasswordUtils.maskPasswordsInDefinition(jobDefinitionResource.getDefinition()));
			maskedJobDefinitionResource.setStatus(jobDefinitionResource.getStatus());
			maskedContents.add(maskedJobDefinitionResource);
		}

		return new PagedResources<JobDefinitionResource>(maskedContents, pagedResources.getMetadata(),
				pagedResources.getLinks());
	}

	@ResponseBody
	@RequestMapping(value = "/clean/rabbit/{job}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public Map<String, List<String>> clean(@PathVariable String job,
			@RequestParam(required = false) String adminUri,
			@RequestParam(required = false) String user,
			@RequestParam(required = false) String pw,
			@RequestParam(required = false) String vhost,
			@RequestParam(required = false) String busPrefix) {
		return cleanRabbitBus(job, adminUri, user, pw, vhost, busPrefix, true);
	}

	@Override
	protected JobDefinition createDefinition(String name, String definition) {
		return new JobDefinition(name, definition);
	}
}
