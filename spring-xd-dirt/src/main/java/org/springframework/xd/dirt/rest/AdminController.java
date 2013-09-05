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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityLinks;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.xd.rest.client.domain.JobDefinitionResource;
import org.springframework.xd.rest.client.domain.StreamDefinitionResource;
import org.springframework.xd.rest.client.domain.XDRuntime;
import org.springframework.xd.rest.client.domain.metrics.AggregateCountsResource;
import org.springframework.xd.rest.client.domain.metrics.CounterResource;
import org.springframework.xd.rest.client.domain.metrics.FieldValueCounterResource;
import org.springframework.xd.rest.client.domain.metrics.GaugeResource;
import org.springframework.xd.rest.client.domain.metrics.RichGaugeResource;

/**
 * @author Eric Bottard
 */
@Controller
@RequestMapping("/")
public class AdminController {

	@Autowired
	public AdminController(EntityLinks entityLinks) {
		this.entityLinks = entityLinks;
	}

	private final EntityLinks entityLinks;

	@RequestMapping()
	@ResponseBody
	public XDRuntime info() {
		XDRuntime xdRuntime = new XDRuntime();
		xdRuntime.add(entityLinks.linkFor(StreamDefinitionResource.class).withRel("streams"));
		xdRuntime.add(entityLinks.linkFor(JobDefinitionResource.class).withRel("jobs"));

		xdRuntime.add(entityLinks.linkFor(CounterResource.class).withRel("counters"));
		xdRuntime.add(entityLinks.linkFor(FieldValueCounterResource.class).withRel("field-value-counters"));
		xdRuntime.add(entityLinks.linkFor(AggregateCountsResource.class).withRel("aggregate-counters"));
		xdRuntime.add(entityLinks.linkFor(GaugeResource.class).withRel("gauges"));
		xdRuntime.add(entityLinks.linkFor(RichGaugeResource.class).withRel("richgauges"));
		return xdRuntime;
	}
}
