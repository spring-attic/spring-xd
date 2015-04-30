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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.VndErrors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.xd.dirt.stream.XDParser;
import org.springframework.xd.module.ModuleDescriptor;

/**
 * A controller that can accept a whole list of definitions and report whether
 * they are valid as a whole or not.
 *
 * @author Eric Bottard
 */
@RestController
@RequestMapping("/flo")
public class FloController {

	private XDParser parser;

	@Autowired
	public FloController(XDParser parser) {
		this.parser = parser;
	}

	@RequestMapping(method = RequestMethod.GET)
	public List<DefinitionOrVndErrors> validate(@RequestParam("definitions") String definitions) {

		List<XDParser.DefinitionOrException> parse = parser.parse(definitions.split("\n"));
		List<DefinitionOrVndErrors> result = new ArrayList<>(parse.size());
		for (XDParser.DefinitionOrException doe : parse) {
			result.add(new DefinitionOrVndErrors(doe));
		}
		return result;
	}

	public static class DefinitionOrVndErrors {

		private static final RestControllerAdvice ADVICE = new RestControllerAdvice();

		private final List<ModuleDescriptor> descriptors;

		private final VndErrors errors;

		private DefinitionOrVndErrors(XDParser.DefinitionOrException doe) {
			descriptors = doe.getDescriptors();
			errors = doe.getException() != null ? ADVICE.onException(doe.getException()) : null;
		}

		public List<ModuleDescriptor> getDescriptors() {
			return descriptors;
		}

		public VndErrors getErrors() {
			return errors;
		}
	}
}
