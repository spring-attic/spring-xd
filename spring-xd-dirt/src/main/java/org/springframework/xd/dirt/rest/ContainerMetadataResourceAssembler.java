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
import org.springframework.xd.dirt.container.ContainerMetadata;
import org.springframework.xd.rest.client.domain.ContainerMetadataResource;

/**
 * Knows how to assemble {@link ContainerMetadataResource}s out of {@link ContainerMetadata} instances.
 * 
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
public class ContainerMetadataResourceAssembler extends
		ResourceAssemblerSupport<ContainerMetadata, ContainerMetadataResource> {

	public ContainerMetadataResourceAssembler() {
		super(RuntimeContainersController.class, ContainerMetadataResource.class);
	}

	@Override
	public ContainerMetadataResource toResource(ContainerMetadata entity) {
		return createResourceWithId(entity.getId(), entity);
	}

	@Override
	protected ContainerMetadataResource instantiateResource(ContainerMetadata entity) {
		return new ContainerMetadataResource(entity.getId(), entity.getPid(), entity.getHost(), entity.getIp());
	}

}
