/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.xd.dirt.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.dirt.module.ModuleNotDeployedException;
import org.springframework.xd.dirt.module.store.ModuleMetadata;
import org.springframework.xd.dirt.module.store.ModuleMetadataRepository;
import org.springframework.xd.rest.domain.ModuleMetadataResource;


/**
 * Controller that handles the interaction with the deployed modules.
 *
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 */
@Controller
@RequestMapping("/runtime/modules")
@ExposesResourceFor(ModuleMetadataResource.class)
public class ModulesMetadataController {

	private final ModuleMetadataRepository moduleMetadataRepository;

	private ResourceAssemblerSupport<ModuleMetadata, ModuleMetadataResource> moduleMetadataResourceAssembler;

	@Autowired
	public ModulesMetadataController(ModuleMetadataRepository moduleMetadataRepository) {
		this.moduleMetadataRepository = moduleMetadataRepository;
		moduleMetadataResourceAssembler = new ModuleMetadataResourceAssembler();
	}

	/**
	 * List module metadata for all the deployed modules.
	 *
	 * @param pageable pagination information
	 * @param assembler paged resource assembler
	 * @return paged {@link ModuleMetadataResource}
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public PagedResources<ModuleMetadataResource> list(Pageable pageable,
			PagedResourcesAssembler<ModuleMetadata> assembler) {

		final Page<ModuleMetadata> page = this.moduleMetadataRepository.findAll(pageable);
		return maskSensitiveData(assembler.toResource(page, moduleMetadataResourceAssembler));
	}

	/**
	 * List the module metadata for all the modules that are deployed to the given container.
	 *
	 * @param pageable pagination information
	 * @param assembler paged resource assembler
	 * @param containerId the container id of the container to choose
	 * @return paged {@link ModuleMetadataResource}
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, params = { "containerId" })
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public PagedResources<ModuleMetadataResource> listByContainer(Pageable pageable,
			PagedResourcesAssembler<ModuleMetadata> assembler,
			@RequestParam("containerId") String containerId) {
		return maskSensitiveData(assembler.toResource(
				this.moduleMetadataRepository.findAllByContainerId(pageable, containerId),
				moduleMetadataResourceAssembler));
	}

	/**
	 * List the module metadata for all the modules with the given moduleId.
	 *
	 * @param pageable pagination information
	 * @param assembler paged resource assembler
	 * @param moduleId the module id of the module metadata to list
	 * @return paged {@link ModuleMetadataResource}
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, params = { "moduleId" })
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public PagedResources<ModuleMetadataResource> listByModule(Pageable pageable,
			PagedResourcesAssembler<ModuleMetadata> assembler,
			@RequestParam("moduleId") String moduleId) {
		return maskSensitiveData(assembler.toResource(
				this.moduleMetadataRepository.findAllByModuleId(pageable, moduleId),
				moduleMetadataResourceAssembler));
	}

	/**
	 * List the module metadata for the given moduleId and deployed to the given containerId.
	 *
	 * @param containerId the container id of the container to choose
	 * @param moduleId the module id of the module metadata to list
	 * @return the {@link ModuleMetadataResource} of the module
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, params = { "containerId", "moduleId" })
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ModuleMetadataResource listByContainerAndModuleId(
			@RequestParam("containerId") String containerId,
			@RequestParam("moduleId") String moduleId) {
		ModuleMetadata moduleMetadata = this.moduleMetadataRepository.findOne(containerId, moduleId);
		if (moduleMetadata == null) {
			throw new ModuleNotDeployedException(containerId, moduleId);
		}
		return maskSensitiveData(moduleMetadataResourceAssembler.toResource(moduleMetadata));

	}

	/**
	 * Will return a collection of {@link ModuleMetadata} that are associated with
	 * the provided {@code jobname} request parameter.
	 *
	 * @param jobName parameter must not be null
	 * @return Collection of {@link ModuleMetadata}, might be empty but never {@code null}
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, params = { "jobname" })
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<ModuleMetadataResource> displayForJobname(@RequestParam("jobname") String jobName) {

		final Iterable<ModuleMetadata> moduleMetadataIterable = this.moduleMetadataRepository.findAll();
		final List<ModuleMetadata> moduleMetadataListToReturn = new ArrayList<ModuleMetadata>();

		for (ModuleMetadata moduleMetadata : moduleMetadataIterable) {
			if (jobName.equals(moduleMetadata.getUnitName())) {
				moduleMetadataListToReturn.add(moduleMetadata);
			}
		}

		final List<ModuleMetadataResource> resources = moduleMetadataResourceAssembler.toResources(moduleMetadataListToReturn);
		maskSensitiveData(resources);
		return resources;
	}

	private ModuleMetadataResource maskSensitiveData(ModuleMetadataResource moduleMetadataResource) {
		PasswordUtils.maskPropertiesIfNecessary(moduleMetadataResource.getModuleOptions());
		return moduleMetadataResource;
	}

	private PagedResources<ModuleMetadataResource> maskSensitiveData(
			PagedResources<ModuleMetadataResource> moduleMetadataResources) {
		maskSensitiveData(moduleMetadataResources.getContent());
		return moduleMetadataResources;
	}

	private Collection<ModuleMetadataResource> maskSensitiveData(
			Collection<ModuleMetadataResource> moduleMetadataResources) {

		for (ModuleMetadataResource metadataResource : moduleMetadataResources) {
			maskSensitiveData(metadataResource);
		}

		return moduleMetadataResources;
	}
}
