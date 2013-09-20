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

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.dirt.module.ModuleRepository;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.rest.client.domain.ModuleDefinitionResource;

/**
 * Handles all Module related interactions.
 * 
 * @author Glenn Renfro
 * @since 1.0
 */
@Controller
@RequestMapping("/modules")
@ExposesResourceFor(ModuleDefinitionResource.class)
public class ModulesController {

	private final ModuleRegistry moduleRegistry;

	@Autowired
	public ModulesController(ModuleRegistry moduleRegistry) {
		this.moduleRegistry = moduleRegistry;
	}

	/**
	 * List Module definitions.
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public PagedResources<ModuleDefinitionResource> list(Pageable pageable,
			PagedResourcesAssembler<ModuleDefinition> assembler,
			@RequestParam(value = "type", required = false) String type) {
		ModuleRepository handler = new ModuleRepository(moduleRegistry);
		Page<ModuleDefinition> page = handler.findAll(pageable, type);
		PagedResources<ModuleDefinitionResource> result = safePagedResources(assembler, page);
		return result;
	}

	private PagedResources<ModuleDefinitionResource> safePagedResources(
			PagedResourcesAssembler<ModuleDefinition> assembler,
			Page<ModuleDefinition> page) {
		if (page.hasContent()) {
			return assembler.toResource(page, new ModuleDefinitionResourceAssembler());
		}
		else {
			return new PagedResources<ModuleDefinitionResource>(
					new ArrayList<ModuleDefinitionResource>(), null);
		}
	}

	/**
	 * Create a new Virtual Module.
	 * 
	 * @param name The name of the module to create (required)
	 * @param definition The module definition, expressed in the XD DSL (required)
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public ModuleDefinitionResource save(@RequestParam("name") String name,
			@RequestParam("definition") String definition) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
