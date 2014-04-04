/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.rest;

import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.xd.dirt.container.ContainerAttributes;
import org.springframework.xd.rest.client.domain.ContainerAttributesResource;

/**
 * Knows how to assemble {@link ContainerAttributesResource}s out of {@link ContainerAttributes} instances.
 * 
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
public class ContainerAttributesResourceAssembler extends
		ResourceAssemblerSupport<ContainerAttributes, ContainerAttributesResource> {

	public ContainerAttributesResourceAssembler() {
		super(RuntimeContainersController.class, ContainerAttributesResource.class);
	}

	@Override
	public ContainerAttributesResource toResource(ContainerAttributes entity) {
		return createResourceWithId(entity.getId(), entity);
	}

	@Override
	protected ContainerAttributesResource instantiateResource(ContainerAttributes entity) {
		return new ContainerAttributesResource(entity.getId(), entity.getPid(), entity.getHost(), entity.getIp());
	}

}
