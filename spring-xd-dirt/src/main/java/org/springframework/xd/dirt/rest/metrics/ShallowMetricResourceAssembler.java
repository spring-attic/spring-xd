/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.xd.analytics.metrics.core.Metric;
import org.springframework.xd.rest.domain.metrics.MetricResource;

/**
 * Base class for a {@link ResourceAssembler} that builds shallow resources for metrics (exposing only their names, and
 * hence their "self" rel).
 * 
 * @author Eric Bottard
 */
public class ShallowMetricResourceAssembler<M extends Metric> extends
		ResourceAssemblerSupport<M, MetricResource> {

	public ShallowMetricResourceAssembler(Class<?> controllerClass) {
		super(controllerClass, MetricResource.class);
	}

	@Override
	public MetricResource toResource(M entity) {
		return createResourceWithId(entity.getName(), entity);
	}

	@Override
	protected MetricResource instantiateResource(M entity) {
		return new MetricResource(entity.getName());
	}

}
