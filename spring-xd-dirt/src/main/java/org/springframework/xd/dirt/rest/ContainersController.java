/*
 * Copyright 2013 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.dirt.container.store.ContainerEntity;
import org.springframework.xd.dirt.container.store.ContainerRepository;
import org.springframework.xd.dirt.module.store.ContainerModulesRepository;
import org.springframework.xd.dirt.module.store.ModuleEntity;
import org.springframework.xd.rest.client.domain.ContainerResource;
import org.springframework.xd.rest.client.domain.ModuleResource;


/**
 * Handles interaction with the available containers/and its modules.
 * 
 * @author Ilayaperumal Gopinathan
 */
@Controller
@RequestMapping("/runtime/containers")
@ExposesResourceFor(ContainerResource.class)
public class ContainersController {

	private ContainerRepository containerRepository;

	private ContainerModulesRepository containerModulesRepository;

	private ResourceAssemblerSupport<ContainerEntity, ContainerResource> containerResourceAssemblerSupport;

	private ResourceAssemblerSupport<ModuleEntity, ModuleResource> moduleResourceAssemblerSupport = new ModuleResourceAssembler();

	@Autowired
	public ContainersController(ContainerRepository containerRepository,
			ContainerModulesRepository containerModulesRepository) {
		this.containerRepository = containerRepository;
		this.containerModulesRepository = containerModulesRepository;
		containerResourceAssemblerSupport = new ContainerResourceAssembler();
	}

	/**
	 * List all the available containers
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public PagedResources<ContainerResource> list(Pageable pageable, PagedResourcesAssembler<ContainerEntity> assembler) {
		Page<ContainerEntity> page = this.containerRepository.findAll(pageable);
		PagedResources<ContainerResource> result = safePagedContainerResources(assembler, page);
		return result;
	}

	/**
	 * List all the available modules by container
	 */
	@RequestMapping(value = "/{containerId}/modules", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Iterable<ModuleResource> listByContainer(@PathVariable("containerId") String containerId) {
		Iterable<ModuleEntity> moduleEntities = this.containerModulesRepository.findAll(containerId);
		return moduleResourceAssemblerSupport.toResources(moduleEntities);
	}

	/*
	 * Work around https://github.com/SpringSource/spring-hateoas/issues/89
	 */
	private PagedResources<ContainerResource> safePagedContainerResources(
			PagedResourcesAssembler<ContainerEntity> assembler,
			Page<ContainerEntity> page) {
		if (page.hasContent()) {
			return assembler.toResource(page, containerResourceAssemblerSupport);
		}
		else {
			return new PagedResources<ContainerResource>(new ArrayList<ContainerResource>(), null);
		}
	}
}
