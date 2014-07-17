/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.rest.metrics;

import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.xd.analytics.metrics.core.Gauge;
import org.springframework.xd.rest.domain.metrics.GaugeResource;

/**
 * Knows how to assemble {@link GaugeResource}s out of {@link Gauge}s.
 * 
 * @author Luke Taylor
 */
class DeepGaugeResourceAssembler extends
		ResourceAssemblerSupport<Gauge, GaugeResource> {

	public DeepGaugeResourceAssembler() {
		super(GaugesController.class, GaugeResource.class);
	}

	@Override
	public GaugeResource toResource(Gauge entity) {
		return createResourceWithId(entity.getName(), entity);
	}

	@Override
	protected GaugeResource instantiateResource(Gauge entity) {
		return new GaugeResource(entity.getName(), entity.getValue());
	}

}
