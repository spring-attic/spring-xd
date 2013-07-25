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

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
import org.springframework.xd.dirt.stream.NoSuchDefinitionException;
import org.springframework.xd.dirt.stream.TriggerDefinition;
import org.springframework.xd.dirt.stream.TriggerDeployer;
import org.springframework.xd.rest.client.domain.TriggerDefinitionResource;

/**
 * Handles all Trigger related interactions.
 *
 * @author Gunnar Hillert
 * @since 1.0
 */
@Controller
@RequestMapping("/triggers")
@ExposesResourceFor(TriggerDefinitionResource.class)
public class TriggersController {

	private final TriggerDeployer triggerDeployer;

	private final TriggerDefinitionResourceAssembler definitionResourceAssembler = new TriggerDefinitionResourceAssembler();

	@Autowired
	public TriggersController(TriggerDeployer streamDeployer) {
		this.triggerDeployer = streamDeployer;
	}

	/**
	 * Request removal of an existing {@link TriggerDefinition}.
	 *
	 * @param name the name of an existing trigger (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void delete(@PathVariable("name") String name) {
		triggerDeployer.delete(name);
	}

	/**
	 * Deploy an existing Trigger.
	 *
	 * @param name the name of the tap to create (required)
	 * @param definition the tap definition expressed in the XD DSL (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.PUT, params = "deploy=true")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void deploy(@PathVariable("name") String name) {
		triggerDeployer.deploy(name);
	}

	/**
	 * Retrieve information about a single {@link TriggerDefinition}.
	 *
	 * @param name the name of an existing trigger (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public TriggerDefinitionResource display(@PathVariable("name") String name) {
		final TriggerDefinition triggerDefinition = triggerDeployer.findOne(name);

		if (triggerDefinition == null) {
			throw new NoSuchDefinitionException(name, "There is no trigger definition named '%s'");
		}
		return definitionResourceAssembler.toResource(triggerDefinition);
	}

	/**
	 * List Trigger definitions.
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public PagedResources<TriggerDefinitionResource> list(Pageable pageable, PagedResourcesAssembler<TriggerDefinition> assembler) {

		final Page<TriggerDefinition> page = triggerDeployer.findAll(pageable);

		if (page.hasContent()) {
			return assembler.toResource(page, definitionResourceAssembler);
		}
		else {
			return new PagedResources<TriggerDefinitionResource>(new ArrayList<TriggerDefinitionResource>(), null);
		}
	}

	/**
	 * Create a new Trigger.
	 *
	 * @param name the name of the trigger to create (required)
	 * @param definition the {@link TriggerDefinition} expressed in the XD DSL (required)
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public TriggerDefinitionResource save(@RequestParam("name") String name,
			@RequestParam("definition") String definition,
			@RequestParam(value = "deploy", defaultValue = "true") boolean deploy) {
		final TriggerDefinition triggerDefinition = new TriggerDefinition(name, definition);
		final TriggerDefinition savedTriggerDefinition = triggerDeployer.save(triggerDefinition);
		final TriggerDefinitionResource result = definitionResourceAssembler.toResource(savedTriggerDefinition);
		if (deploy) {
			triggerDeployer.deploy(name);
		}
		return result;
	}

	/**
	 * Request removal of an existing trigger.
	 *
	 * @param name the name of an existing trigger (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.PUT, params = "deploy=false")
	@ResponseStatus(HttpStatus.OK)
	public void undeploy(@PathVariable("name") String name) {
		throw new NotImplementedException("Removal of Triggers is not Implemented, yet.");
	}
}

