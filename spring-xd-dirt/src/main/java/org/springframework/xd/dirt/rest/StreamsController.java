/*
 * Copyright 2013-2015 the original author or authors.
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
import java.util.List;
import java.util.Map;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentUnitType;
import org.springframework.xd.dirt.stream.Stream;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.stream.StreamDeployer;
import org.springframework.xd.rest.domain.StreamDefinitionResource;

/**
 * Handles all Stream related interaction.
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 * @author David Turanski
 * @author Gary Russell
 *
 * @since 1.0
 */
@Controller
@RequestMapping("/streams")
@ExposesResourceFor(StreamDefinitionResource.class)
public class StreamsController extends
		XDController<StreamDefinition, StreamDefinitionResourceAssembler, StreamDefinitionResource, Stream> {

	@Autowired
	public StreamsController(StreamDeployer streamDeployer) {
		super(streamDeployer, new StreamDefinitionResourceAssembler(), DeploymentUnitType.Stream);
	}

	/**
	 * List stream definitions.
	 */
	@ResponseBody
	@RequestMapping(value = "/definitions", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<StreamDefinitionResource> list(Pageable pageable,
			PagedResourcesAssembler<StreamDefinition> assembler) {

		PagedResources<StreamDefinitionResource> pagedResources = listValues(pageable, assembler);

		final List<StreamDefinitionResource> maskedContents = new ArrayList<StreamDefinitionResource>(
				pagedResources.getContent().size());

		for (StreamDefinitionResource streamDefinitionResource : pagedResources.getContent()) {
			streamDefinitionResource.getDefinition();
			StreamDefinitionResource maskedStreamDefinitionResource =
					new StreamDefinitionResource(streamDefinitionResource.getName(),
							PasswordUtils.maskPasswordsInDefinition(streamDefinitionResource.getDefinition()));
			maskedStreamDefinitionResource.setStatus(streamDefinitionResource.getStatus());
			maskedContents.add(maskedStreamDefinitionResource);
		}

		return new PagedResources<StreamDefinitionResource>(maskedContents, pagedResources.getMetadata(),
				pagedResources.getLinks());

	}

	@Override
	protected StreamDefinition createDefinition(String name, String definition) {
		return new StreamDefinition(name, definition);
	}

	@ResponseBody
	@RequestMapping(value = "/clean/rabbit/{stream}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public Map<String, List<String>> clean(@PathVariable String stream,
			@RequestParam(required = false) String adminUri,
			@RequestParam(required = false) String user,
			@RequestParam(required = false) String pw,
			@RequestParam(required = false) String vhost,
			@RequestParam(required = false) String busPrefix) {
		return cleanRabbitBus(stream, adminUri, user, pw, vhost, busPrefix, false);
	}

}
