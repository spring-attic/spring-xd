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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.springframework.xd.dirt.stream.TapDefinition;
import org.springframework.xd.dirt.stream.TapDeployer;
import org.springframework.xd.rest.client.domain.TapDefinitionResource;

/**
 * @author David Turanski
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
	 * Create a new Tap.
	 * 
	 * @param name the name of the tap to create (required)
	 * @param definition the tap definition expressed in the XD DSL (required)
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public TapDefinitionResource create(@RequestParam("name") String name, @RequestParam("definition") String definition,
			@RequestParam(value = "control", defaultValue = "") String control) {
		String streamName = getStreamName(name, definition);
		tapDeployer.create(new TapDefinition(name, streamName, definition));
		//TODO: Optional deploy
		TapDefinitionResource result = new TapDefinitionResource(name, streamName, definition);
		if ("start".equalsIgnoreCase(control)) {
			tapDeployer.deploy(name);
		}
		return result;

	}

	/**
	 * @param definition
	 * @return
	 */
	String getStreamName(String name, String definition) {
		Pattern pattern = Pattern.compile("^\\s*tap\\s*@{0,1}\\s*(\\w+).*");
		Matcher matcher = pattern.matcher(definition);
		if (matcher.matches()) {		
			return matcher.group(1);
		}
		return null;
		
	}

	/**
	 * Deploy an existing Tap.
	 * 
	 * @param name the name of the tap to create (required)
	 * @param definition the tap definition expressed in the XD DSL (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.PUT)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void deploy(@PathVariable("name") String name) {
		tapDeployer.deploy(name);
	}
}
