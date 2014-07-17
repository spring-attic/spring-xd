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

import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.rest.domain.StreamDefinitionResource;

/**
 * Knows how to build a REST resource out of our domain model {@link StreamDefinition}.
 * 
 * @author Eric Bottard
 */
public class StreamDefinitionResourceAssembler extends
		ResourceAssemblerSupport<StreamDefinition, StreamDefinitionResource> {

	public StreamDefinitionResourceAssembler() {
		super(StreamsController.class, StreamDefinitionResource.class);
	}

	@Override
	public StreamDefinitionResource toResource(StreamDefinition entity) {
		return createResourceWithId(entity.getName(), entity);
	}

	@Override
	protected StreamDefinitionResource instantiateResource(StreamDefinition entity) {
		return new StreamDefinitionResource(entity.getName(), entity.getDefinition());
	}

}
