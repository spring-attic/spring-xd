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
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.dirt.stream.ParsingContext;
import org.springframework.xd.dirt.stream.XDStreamParser;
import org.springframework.xd.dirt.stream.dsl.ModuleNode;
import org.springframework.xd.rest.domain.ModuleDescriptorResource;

import com.google.common.collect.Lists;

/**
 * Stream/Job DSL parser controller.
 *
 * @author Ilayaperumal Gopinathan
 */
@Controller
@RequestMapping("/dslparser")
@ExposesResourceFor(ModuleDescriptorResource.class)
public class StreamConfigParserController {

	@Autowired
	XDStreamParser parser;

	private ModuleDescriptorResourceAssembler resourceAssembler = new ModuleDescriptorResourceAssembler();

	/**
	 * List all the {@link ModuleNode}s from the DSL.
	 */
	@ResponseBody
	@RequestMapping(value = "", method = RequestMethod.GET, params = { "dsl" })
	@ResponseStatus(HttpStatus.OK)
	public List<ModuleDescriptorResource> get(@RequestParam("dsl") String dsl) {
		//TODO: add other parsing contexts.
		return resourceAssembler.toResources(Lists.reverse(parser.parse("dummy", dsl, ParsingContext.stream)));
	}
}
