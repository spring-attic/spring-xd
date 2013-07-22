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
import org.springframework.xd.dirt.stream.NoSuchDefinitionException;
import org.springframework.xd.dirt.stream.TapDefinition;
import org.springframework.xd.dirt.stream.TapDeployer;
import org.springframework.xd.rest.client.domain.TapDefinitionResource;

/**
 * @author David Turanski
 * @author Gunnar Hillert
 *
 * @since 1.0
 */
@Controller
@RequestMapping("/taps")
@ExposesResourceFor(TapDefinitionResource.class)
public class TapsController {

	private final TapDeployer tapDeployer;

	private final TapDefinitionResourceAssembler definitionResourceAssembler = new TapDefinitionResourceAssembler();

	@Autowired
	public TapsController(TapDeployer tapDeployer) {
		this.tapDeployer = tapDeployer;
	}

	/**
	 * Request removal of an existing tap.
	 *
	 * @param name the name of an existing tap (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void delete(@PathVariable("name") String name) {
		tapDeployer.delete(name);
	}

	/**
	 * Deploy an existing Tap.
	 *
	 * @param name the name of the tap to deploy (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.PUT, params = "deploy=true")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void deploy(@PathVariable("name") String name) {
		tapDeployer.deploy(name);
	}

	/**
	 * Retrieve information about a single {@link TapDefinition}.
	 *
	 * @param name the name of an existing tap (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public TapDefinitionResource display(@PathVariable("name") String name) {
		final TapDefinition tapDefinition = tapDeployer.findOne(name);

		if (tapDefinition == null) {
			throw new NoSuchDefinitionException(name, "There is no tap definition named '%s'");
		}

		return definitionResourceAssembler.toResource(tapDefinition);
	}

	/**
	 * List Tap definitions.
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Iterable<TapDefinitionResource> list() {
		final Iterable<TapDefinition> taps = tapDeployer.findAll();
		return definitionResourceAssembler.toResources(taps);
	}

	/**
	 * Create a new Tap.
	 *
	 * @param name the name of the tap to create (required)
	 * @param definition the tap definition expressed in the XD DSL (required)
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public TapDefinitionResource save(@RequestParam("name") String name,
			@RequestParam("definition") String definition,
			@RequestParam(value = "deploy", defaultValue = "true") boolean deploy) {
		final TapDefinition tapDefinition = new TapDefinition(name, definition);
		final TapDefinition savedTapDefinition = tapDeployer.save(tapDefinition);
		final TapDefinitionResource result = definitionResourceAssembler.toResource(savedTapDefinition);
		if (deploy) {
			tapDeployer.deploy(name);
		}
		return result;
	}

	/**
	 * Request un-deployment of an existing named tap.
	 *
	 * @param name the name of an existing tap (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.PUT, params = "deploy=false")
	@ResponseStatus(HttpStatus.OK)
	public void undeploy(@PathVariable("name") String name) {
		tapDeployer.undeploy(name);
	}

}
