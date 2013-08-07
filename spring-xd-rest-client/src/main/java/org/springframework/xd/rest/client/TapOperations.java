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

package org.springframework.xd.rest.client;

import org.springframework.hateoas.PagedResources;
import org.springframework.xd.rest.client.domain.TapDefinitionResource;

/**
 * Interface defining operations against taps.
 *
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 *
 * @since 1.0
 */
public interface TapOperations {

	/**
	 * Create a new Tap.
	 *
	 * @param name the name to give to the tap
	 * @param definition the tap definition, expressed in XD DSL
	 * @param deploy whether to deploy the tap immediately
	 * @return a runtime representation of the created tap
	 */
	public TapDefinitionResource createTap(String name, String definition, boolean deploy);

	/**
	 * Retrieve a list of taps.
	 *
	 * @return A list of taps
	 */
	PagedResources<TapDefinitionResource> listTaps(/* TODO */);

	/**
	 * Destroy an existing tap
	 * @param name
	 */
	public void destroyTap(String name);

}
