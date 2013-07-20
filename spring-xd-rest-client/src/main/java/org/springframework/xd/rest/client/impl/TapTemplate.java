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

package org.springframework.xd.rest.client.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.xd.rest.client.TapOperations;
import org.springframework.xd.rest.client.domain.TapDefinitionResource;

/**
 * Implementation of the Tap related part of the API.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 *
 * @since 1.0
 */
public class TapTemplate extends AbstractTemplate implements TapOperations {

	TapTemplate(AbstractTemplate source) {
		super(source);
	}

	@Override
	public TapDefinitionResource createTap(String name, String definition, boolean deploy) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("name", name);
		values.add("definition", definition);
		values.add("deploy", Boolean.toString(deploy));

		TapDefinitionResource tap = restTemplate.postForObject(resources.get("taps"), values,
				TapDefinitionResource.class);
		return tap;
	}

	/**
	 * Returns a {@link List} of {@link TapDefinitionResource}s.
	 */
	@Override
	public List<TapDefinitionResource> listTaps() {
		final TapDefinitionResource[] taps = restTemplate.getForObject(resources.get("taps"), TapDefinitionResource[].class);
		return Arrays.asList(taps);
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.xd.rest.client.TapOperations#destroyTap(java.lang.String)
	 */
	@Override
	public void destroyTap(String name) {
		String uriTemplate = resources.get("taps").toString() + "/{name}";
		restTemplate.delete(uriTemplate, Collections.singletonMap("name", name));
	}

}
