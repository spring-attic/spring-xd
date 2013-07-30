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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
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
public class TriggersController
		extends
		XDController<TriggerDefinition, TriggerDefinitionResourceAssembler, TriggerDefinitionResource> {

	@Autowired
	public TriggersController(TriggerDeployer triggerDeployer) {
		super(triggerDeployer, new TriggerDefinitionResourceAssembler());
	}

	/**
	 * List Trigger definitions.
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public PagedResources<TriggerDefinitionResource> list(Pageable pageable,
			PagedResourcesAssembler<TriggerDefinition> assembler) {

		return listValues(pageable, assembler);
	}

	/**
	 * Request removal of an existing trigger.
	 *
	 * @param name
	 *            the name of an existing trigger (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.PUT, params = "deploy=false")
	@ResponseStatus(HttpStatus.OK)
	public void undeploy(@PathVariable("name") String name) {
		throw new NotImplementedException(
				"Removal of Triggers is not Implemented, yet.");
	}

	protected TriggerDefinition definitionFactory(String name, String definition) {
		return new TriggerDefinition(name, definition);
	}
}
