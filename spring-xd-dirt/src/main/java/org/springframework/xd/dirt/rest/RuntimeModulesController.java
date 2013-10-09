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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.dirt.module.store.RuntimeContainerModuleInfoRepository;
import org.springframework.xd.dirt.module.store.RuntimeModuleInfoEntity;
import org.springframework.xd.dirt.module.store.RuntimeModuleInfoRepository;
import org.springframework.xd.rest.client.domain.RuntimeModuleInfoResource;


/**
 * Controller that handles the interaction with the deployed modules.
 * 
 * @author Ilayaperumal Gopinathan
 */
@Controller
@RequestMapping("/runtime/modules")
@ExposesResourceFor(RuntimeModuleInfoResource.class)
public class RuntimeModulesController {

	private RuntimeModuleInfoRepository runtimeModuleInfoRepository;

	private RuntimeContainerModuleInfoRepository runtimeContainerModuleInfoRepository;

	private ResourceAssemblerSupport<RuntimeModuleInfoEntity, RuntimeModuleInfoResource> runtimeModuleResourceAssemblerSupport;

	@Autowired
	public RuntimeModulesController(RuntimeModuleInfoRepository runtimeModuleInfoRepository,
			RuntimeContainerModuleInfoRepository runtimeContainerModuleInfoRepository) {
		this.runtimeModuleInfoRepository = runtimeModuleInfoRepository;
		this.runtimeContainerModuleInfoRepository = runtimeContainerModuleInfoRepository;
		runtimeModuleResourceAssemblerSupport = new RuntimeModuleInfoResourceAssembler();
	}

	/**
	 * List all the available modules
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public PagedResources<RuntimeModuleInfoResource> list(Pageable pageable,
			PagedResourcesAssembler<RuntimeModuleInfoEntity> assembler,
			@RequestParam(value = "containerId", required = false) String containerId) {
		Page<RuntimeModuleInfoEntity> page;
		if (containerId != null) {
			page = this.runtimeContainerModuleInfoRepository.findAllByContainerId(pageable, containerId);
		}
		else {
			page = this.runtimeModuleInfoRepository.findAll(pageable);
		}
		PagedResources<RuntimeModuleInfoResource> result = safePagedResources(assembler, page);
		return result;
	}

	/*
	 * Work around https://github.com/SpringSource/spring-hateoas/issues/89
	 */
	private PagedResources<RuntimeModuleInfoResource> safePagedResources(
			PagedResourcesAssembler<RuntimeModuleInfoEntity> assembler,
			Page<RuntimeModuleInfoEntity> page) {
		if (page.hasContent()) {
			return assembler.toResource(page, runtimeModuleResourceAssemblerSupport);
		}
		else {
			return new PagedResources<RuntimeModuleInfoResource>(new ArrayList<RuntimeModuleInfoResource>(), null);
		}
	}

}
