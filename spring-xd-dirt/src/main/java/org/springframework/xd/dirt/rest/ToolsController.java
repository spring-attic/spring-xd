/*
 * Copyright 2015 the original author or authors.
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
 *
 *
 */

package org.springframework.xd.dirt.rest;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.xd.dirt.job.dsl.Graph;
import org.springframework.xd.dirt.job.dsl.JobParser;
import org.springframework.xd.dirt.job.dsl.JobSpecificationException;
import org.springframework.xd.dirt.stream.DocumentParseResult;
import org.springframework.xd.dirt.stream.XDStreamParser;
import org.springframework.xd.rest.domain.DocumentParseResultResource;

/**
 * A controller for integrating with frontend tools.
 *
 * @author Eric Bottard
 * @author Andy Clement
 * @author Alex Boyko
 */
@RestController
@RequestMapping("/tools")
public class ToolsController {

	private XDStreamParser.MultiLineDocumentParser multilineStreamParser;

	private DocumentParseResultResourceAssembler assembler = new DocumentParseResultResourceAssembler();

	@Autowired
	public ToolsController(XDStreamParser streamParser) {
		this.multilineStreamParser = new XDStreamParser.MultiLineDocumentParser(streamParser);
	}

	/**
	 * Accept a whole list of definitions and report whether they are valid as a whole or not.
	 */
	@RequestMapping(value = "/parse", method = RequestMethod.GET)
	public DocumentParseResultResource validate(@RequestParam("definitions") String definitions) {
		DocumentParseResult parse = multilineStreamParser.parse(definitions.split("\n"));
		return assembler.toResource(parse);
	}

	/**
	 * Parse a single job specification into a graph structure.
	 */
	@RequestMapping(value = "/parseJobToGraph", method = RequestMethod.POST)
	public Map<String, Object> parseJobToGraph(@RequestBody String specification) {
		Map<String, Object> response = new HashMap<>();
		JobParser jobParser = new JobParser();
		try {
			Graph graph = jobParser.getGraph(specification);
			response.put("graph", graph);
		}
		catch (JobSpecificationException jse) {
			response.put("error", jse.toExceptionDescriptor());
		}
		return response;
	}

	/**
	 * Convert a graph format into DSL text format.
	 */
	@RequestMapping(value = "/convertJobGraphToText", method = RequestMethod.POST)
	public Map<String, Object> convertJobGrabToText(@RequestBody Graph graph) {
		Map<String, Object> response = new HashMap<>();
		try {
			String dslText = graph.toDSLText();
			response.put("text", dslText);
		}
		catch (JobSpecificationException jse) {
			response.put("error", jse.toExceptionDescriptor());
		}
		catch (Throwable e) {
			response.put("error", e.toString());
		}
		return response;
	}

}
