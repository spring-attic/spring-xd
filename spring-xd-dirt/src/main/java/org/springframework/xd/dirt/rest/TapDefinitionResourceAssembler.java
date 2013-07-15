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
import org.springframework.xd.dirt.stream.TapDefinition;
import org.springframework.xd.rest.client.domain.TapDefinitionResource;

/**
 * Knows how to build a REST resource out of our domain model {@link TapDefinition}.
 * 
 * @author David Turanski
 */
public class TapDefinitionResourceAssembler extends ResourceAssemblerSupport<TapDefinition, TapDefinitionResource> {

	public TapDefinitionResourceAssembler() {
		super(TapsController.class, TapDefinitionResource.class);
	}

	@Override
	public TapDefinitionResource toResource(TapDefinition entity) {
		return createResourceWithId(entity.getName(), entity);
	}

	@Override
	protected TapDefinitionResource instantiateResource(TapDefinition entity) {
		return new TapDefinitionResource(entity.getName(), entity.getStreamName(), entity.getDefinition());
	}

}
