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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.dirt.integration.bus.RabbitBusCleaner;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamDeployer;
import org.springframework.xd.rest.domain.StreamDefinitionResource;

/**
 * Handles all Stream related interaction.
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 * @author David Turanski
 *
 * @since 1.0
 */
@Controller
@RequestMapping("/streams")
@ExposesResourceFor(StreamDefinitionResource.class)
public class StreamsController extends
		XDController<StreamDefinition, StreamDefinitionResourceAssembler, StreamDefinitionResource> {

	private final RabbitBusCleaner busCleaner = new RabbitBusCleaner();

	@Autowired
	public StreamsController(StreamDeployer streamDeployer, StreamDefinitionRepository streamDefinitionRepository) {
		super(streamDeployer, new StreamDefinitionResourceAssembler());
	}

	/**
	 * List stream definitions.
	 */
	@ResponseBody
	@RequestMapping(value = "/definitions", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<StreamDefinitionResource> list(Pageable pageable,
			PagedResourcesAssembler<StreamDefinition> assembler) {
		return listValues(pageable, assembler);
	}

	@Override
	protected StreamDefinition createDefinition(String name, String definition) {
		return new StreamDefinition(name, definition);
	}

	@ResponseBody
	@RequestMapping(value = "/clean/{transport}/{stream}")
	@ResponseStatus(HttpStatus.OK)
	public List<String> clean(@PathVariable String transport, @PathVariable String stream,
			@RequestParam(required = false) String adminUri,
			@RequestParam(required = false) String user,
			@RequestParam(required = false) String pw,
			@RequestParam(required = false) String vhost,
			@RequestParam(required = false) String busPrefix) {
		Assert.isTrue("rabbit".equals(transport), "Only rabbit transports can be cleaned");
		String adminUri2 = adminUri == null ? "http://localhost:15672" : adminUri;
		String user2 = user == null ? "guest" : user;
		String pw2 = pw == null ? "guest" : pw;
		String vhost2 = vhost == null ? "/" : vhost;
		String busPrefix2 = busPrefix == null ? "xdbus." : busPrefix;
		List<String> results = busCleaner.clean(adminUri2, user2, pw2, vhost2, busPrefix2, stream);
		if (results.size() == 0) {
			results.add("No queues named " + busPrefix2 + stream + ".* found");
		}
		return results;
	}

}
