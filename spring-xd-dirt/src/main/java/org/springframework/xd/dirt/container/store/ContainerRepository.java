/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.xd.dirt.container.store;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.store.DomainRepository;

/**
 * Repository for persisting {@link Container} entities.
 *
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 */
public interface ContainerRepository extends DomainRepository<Container, String> {

	/**
	 * Update the container attributes.
	 *
	 * @param container the container entity
	 */
	public void update(Container container);

	/**
	 * @param pageable pagination info
	 * @param maskSensitiveProperties Shall sensitive data such as passwords be masked?
	 * @return paged list of all the {@code RuntimeContainer}s in the XD cluster.
	 */
	public Page<DetailedContainer> findAllRuntimeContainers(Pageable pageable, boolean maskSensitiveProperties);

}
