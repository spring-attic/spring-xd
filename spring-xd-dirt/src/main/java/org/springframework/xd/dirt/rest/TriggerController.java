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

import org.apache.commons.lang.NotImplementedException;
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
import org.springframework.xd.dirt.stream.NoSuchStreamException;
import org.springframework.xd.dirt.stream.TriggerDefinition;
import org.springframework.xd.dirt.stream.TriggerDefinitionRepository;
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
public class TriggerController {

	private final TriggerDeployer triggerDeployer;

	private final TriggerDefinitionRepository triggerDefinitionRepository;

	private final TriggerDefinitionResourceAssembler definitionResourceAssembler = new TriggerDefinitionResourceAssembler();

	@Autowired
	public TriggerController(TriggerDeployer streamDeployer, TriggerDefinitionRepository streamDefinitionRepository) {
		this.triggerDeployer = streamDeployer;
		this.triggerDefinitionRepository = streamDefinitionRepository;
	}

	/**
	 * Create a new Trigger.
	 *
	 * @param name The name of the trigger to create (required)
	 * @param definition The Trigger definition, expressed in the XD DSL (required)
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public TriggerDefinitionResource deploy(@RequestParam("name")
	String name, @RequestParam("definition")
	String definition) {
		TriggerDefinition triggerDefinition = new TriggerDefinition(name, definition);
		TriggerDefinition streamDefinition = triggerDeployer.create(triggerDefinition);
		triggerDeployer.deploy(name);
		TriggerDefinitionResource result = definitionResourceAssembler.toResource(streamDefinition);
		return result;
	}

	/**
	 * Retrieve information about a single {@link TriggerDefinition}.
	 *
	 * @param name the name of an existing trigger (required)
	 */
	@ResponseBody
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public TriggerDefinitionResource display(@PathVariable("name")
	String name) {
		TriggerDefinition def = triggerDefinitionRepository.findOne(name);
		return definitionResourceAssembler.toResource(def);
	}

	/**
	 * Request removal of an existing trigger.
	 *
	 * @param name the name of an existing trigger (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void undeploy(@PathVariable("name")
	String name) {
		throw new NotImplementedException("Removal of Triggers is not Implemented, yet.");
	}

	// ---------------- Exception Handlers ------------------------

	/**
	 * Handles the case where client referenced an unknown entity.
	 */
	@ResponseBody
	@ExceptionHandler
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public VndError onNoSuchStreamException(NoSuchStreamException e) {
		return new VndError("NoSuchStreamException", e.getMessage());
	}

}
