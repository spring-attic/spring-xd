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

import com.google.common.collect.Lists;

import org.springframework.hateoas.ResourceAssembler;
import org.springframework.xd.dirt.stream.DocumentParseResult;
import org.springframework.xd.dirt.stream.dsl.StreamDefinitionException;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.rest.domain.DocumentParseResultResource;
import org.springframework.xd.rest.domain.RESTModuleType;

/**
 * This class is responsible for creating a REST representation of a DocumentParseResult.
 *
 * @author Eric Bottard
 */
public class DocumentParseResultResourceAssembler implements ResourceAssembler<DocumentParseResult, DocumentParseResultResource> {

	@Override
	public DocumentParseResultResource toResource(DocumentParseResult entity) {
		DocumentParseResultResource resource = new DocumentParseResultResource();
		for (DocumentParseResult.Line line : entity) {
			DocumentParseResultResource.Line resourceLine = new DocumentParseResultResource.Line();

			// Add any exceptions to the response for this line
			if (line.getExceptions() != null) {

				for (Exception e : line.getExceptions()) {

					if (e instanceof StreamDefinitionException) {
						StreamDefinitionException sde = (StreamDefinitionException) e;
						resourceLine.addError(new DocumentParseResultResource.Error(sde.getMessage(), sde.getPosition()));
					}
					else {
						resourceLine.addError(new DocumentParseResultResource.Error(e.getMessage()));
					}
				}
			}
			// If any modules were parsed, include that in the response
			if (line.getDescriptors() != null) {
				for (ModuleDescriptor md : Lists.reverse(line.getDescriptors())) {
					resourceLine.addDescriptor(new DocumentParseResultResource.ModuleDescriptor(
							md.getGroup(),
							md.getModuleLabel(),
							RESTModuleType.valueOf(md.getType().name()),
							md.getModuleName(),
							md.getSourceChannelName(),
							md.getSinkChannelName(),
							md.getParameters()));
				}
			}
			resource.addLine(resourceLine);
		}
		return resource;
	}
}
