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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.dirt.stream.NoSuchDefinitionException;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamDeployer;
import org.springframework.xd.rest.client.domain.StreamDefinitionResource;

/**
 * Handles all Stream related interaction.
 * 
 * @author Eric Bottard
 * @author Gunnar Hillert
 * 
 * @since 1.0
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
	 * Request removal of an existing stream.
	 * 
	 * @param name the name of an existing stream (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void delete(@PathVariable("name")
	String name) {
		streamDeployer.destroyStream(name);
	}

	/**
	 * Request deployment of an existing named stream.
	 * 
	 * @param name the name of an existing stream (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.PUT, params = "deploy=true")
	@ResponseStatus(HttpStatus.OK)
	public void deploy(@PathVariable("name")
	String name) {
		streamDeployer.deployStream(name);
	}

	/**
	 * Retrieve information about a single {@link StreamDefinition}.
	 * 
	 * @param name the name of an existing stream (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public StreamDefinitionResource display(@PathVariable("name")
	String name) {
		StreamDefinition streamDefinition = streamDefinitionRepository.findOne(name);
		if (streamDefinition == null) {
			throw new NoSuchDefinitionException(name, "There is no stream definition named '%s'");
		}
		return definitionResourceAssembler.toResource(streamDefinition);
	}

	/**
	 * List stream definitions.
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<StreamDefinitionResource> list(Pageable pageable,
			PagedResourcesAssembler<StreamDefinition> assembler) {
		Page<StreamDefinition> page = streamDefinitionRepository.findAll(pageable);
		// Workaround https://github.com/SpringSource/spring-hateoas/issues/89
		if (page.hasContent()) {
			return assembler.toResource(page, definitionResourceAssembler);
		}
		else {
			return new PagedResources<StreamDefinitionResource>(new ArrayList<StreamDefinitionResource>(), null);
		}
	}

	/**
	 * Create a new Stream, optionally deploying it.
	 * 
	 * @param name the name of the stream to create (required)
	 * @param definition some representation of the stream behavior, expressed in the XD DSL (required)
	 * @param deploy whether to also immediately deploy the stream (default true)
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public StreamDefinitionResource save(@RequestParam("name")
	String name, @RequestParam("definition")
	String definition, @RequestParam(value = "deploy", defaultValue = "true")
	boolean deploy) {
		StreamDefinition streamDefinition = streamDeployer.createStream(name, definition, deploy);
		StreamDefinitionResource result = definitionResourceAssembler.toResource(streamDefinition);
		return result;
	}

	/**
	 * Request un-deployment of an existing named stream.
	 * 
	 * @param name the name of an existing stream (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.PUT, params = "deploy=false")
	@ResponseStatus(HttpStatus.OK)
	public void undeploy(@PathVariable("name")
	String name) {
		streamDeployer.undeployStream(name);
	}
}
