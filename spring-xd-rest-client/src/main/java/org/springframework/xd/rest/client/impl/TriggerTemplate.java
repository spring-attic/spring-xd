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

import java.util.Collections;
import java.util.List;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.xd.rest.client.TriggerOperations;
import org.springframework.xd.rest.client.domain.TriggerDefinitionResource;

/**
 * Implementation of the Trigger related part of the API.
 *
 * @author Ilayaperumal Gopinathan
 * @since 1.0
 */
public class TriggerTemplate extends AbstractTemplate implements TriggerOperations {

	TriggerTemplate(AbstractTemplate source) {
		super(source);
	}

	@Override
	public TriggerDefinitionResource createTrigger(String name, String definition) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("name", name);
		values.add("definition", definition);

		TriggerDefinitionResource trigger = restTemplate.postForObject(resources.get("triggers"), values,
				TriggerDefinitionResource.class);
		return trigger;
	}

	/**
	 * Returns a {@link List} of {@link TriggerDefinitionResource}s.
	 */
	@Override
	public TriggerDefinitionResource.Page list() {
		String uriTemplate = resources.get("triggers").toString();
		// TODO handle pagination at the client side
		uriTemplate = uriTemplate + "?size=10000";
		return restTemplate.getForObject(uriTemplate, TriggerDefinitionResource.Page.class);
	}
	@Override
	public void deleteTrigger(String name) {
		String uriTemplate = resources.get("triggers").toString() + "/{name}";
		restTemplate
				.delete(uriTemplate, Collections.singletonMap("name", name));
	}

}
