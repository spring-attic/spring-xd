/*
 * Copyright 2013-2014 the original author or authors.
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
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.xd.rest.client.domain.CompletionKind;
import org.springframework.xd.rest.client.domain.ContainerMetadataResource;
import org.springframework.xd.rest.client.domain.DetailedJobInfoResource;
import org.springframework.xd.rest.client.domain.JobDefinitionResource;
import org.springframework.xd.rest.client.domain.JobExecutionInfoResource;
import org.springframework.xd.rest.client.domain.JobInstanceInfoResource;
import org.springframework.xd.rest.client.domain.ModuleDefinitionResource;
import org.springframework.xd.rest.client.domain.ModuleMetadataResource;
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
		xdRuntime.add(entityLinks.linkFor(ModuleDefinitionResource.class).withRel("modules"));

		xdRuntime.add(entityLinks.linkFor(ModuleMetadataResource.class).withRel("runtime/modules"));
		xdRuntime.add(entityLinks.linkFor(ContainerMetadataResource.class).withRel("runtime/containers"));

		xdRuntime.add(entityLinks.linkFor(DetailedJobInfoResource.class).withRel("batch/jobs"));
		xdRuntime.add(entityLinks.linkFor(JobExecutionInfoResource.class).withRel("batch/executions"));
		xdRuntime.add(entityLinks.linkFor(JobInstanceInfoResource.class).withRel("batch/instances"));

		for (CompletionKind k : CompletionKind.values()) {
			Object mi = ControllerLinkBuilder.methodOn(CompletionsController.class).completions(k, "");
			Link link = ControllerLinkBuilder.linkTo(mi).withRel(String.format("completions/%s", k));
			xdRuntime.add(link);
		}


		xdRuntime.add(entityLinks.linkFor(CounterResource.class).withRel("counters"));
		xdRuntime.add(entityLinks.linkFor(FieldValueCounterResource.class).withRel("field-value-counters"));
		xdRuntime.add(entityLinks.linkFor(AggregateCountsResource.class).withRel("aggregate-counters"));
		xdRuntime.add(entityLinks.linkFor(GaugeResource.class).withRel("gauges"));
		xdRuntime.add(entityLinks.linkFor(RichGaugeResource.class).withRel("rich-gauges"));
		return xdRuntime;
	}
}
