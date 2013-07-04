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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.VndErrors.VndError;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.dirt.stream.NoSuchStreamException;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamDeployer;
import org.springframework.xd.rest.client.domain.StreamDefinitionResource;

/**
 * Handles all Stream related interaction.
 * 
 * @author Eric Bottard
 */
@Controller
@RequestMapping("/streams")
@ExposesResourceFor(StreamDefinitionResource.class)
public class StreamsController {

	private final StreamDeployer streamDeployer;

	private final StreamDefinitionRepository streamDefinitionRepository;

	private final StreamDefinitionResourceAssembler definitionResourceAssembler = new StreamDefinitionResourceAssembler();

	@Autowired
	public StreamsController(StreamDeployer streamDeployer, StreamDefinitionRepository streamDefinitionRepository) {
		this.streamDeployer = streamDeployer;
		this.streamDefinitionRepository = streamDefinitionRepository;
	}

	/**
	 * Create a new Stream / Tap.
	 * 
	 * @param name the name of the stream to create (required)
	 * @param dsl some representation of the stream behavior (required)
	 * @deprecated use POST on /streams instead
	 */
	@Deprecated
	@RequestMapping(value = "/{name}", method = { RequestMethod.PUT })
	@ResponseStatus(HttpStatus.CREATED)
	public void olddeploy(@PathVariable("name")
	String name, @RequestBody
	String dsl) {
		deploy(name, dsl);
	}

	/**
	 * Create a new Stream / Tap.
	 * 
	 * @param name the name of the stream to create (required)
	 * @param definition some representation of the stream behavior, expressed in the XD DSL (required)
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public StreamDefinitionResource deploy(@RequestParam("name")
	String name, @RequestParam("definition")
	String definition) {
		StreamDefinition streamDefinition = streamDeployer.deployStream(name, definition);
		StreamDefinitionResource result = definitionResourceAssembler.toResource(streamDefinition);
		return result;
	}

	/**
	 * Retrieve information about a single {@link StreamDefinition}.
	 * 
	 * @param name the name of an existing stream (required)
	 */
	@ResponseBody
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public StreamDefinitionResource display(@PathVariable("name")
	String name) {
		StreamDefinition def = streamDefinitionRepository.findOne(name);
		return definitionResourceAssembler.toResource(def);
	}

	/**
	 * Request removal of an existing stream.
	 * 
	 * @param name the name of an existing stream (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void undeploy(@PathVariable("name")
	String name) {
		streamDeployer.undeployStream(name);
	}

	/**
	 * List stream definitions.
	 */
	@ResponseBody
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<StreamDefinitionResource> list(Pageable pageable,
			PagedResourcesAssembler<StreamDefinition> assembler) {
		Page<StreamDefinition> page = streamDefinitionRepository.findAll(pageable);
		return assembler.toResource(page, definitionResourceAssembler);
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
