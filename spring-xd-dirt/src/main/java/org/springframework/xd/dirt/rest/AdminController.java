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
import org.springframework.hateoas.TemplateVariable;
import org.springframework.hateoas.TemplateVariable.VariableType;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.xd.rest.domain.CompletionKind;
import org.springframework.xd.rest.domain.DetailedContainerResource;
import org.springframework.xd.rest.domain.DetailedJobInfoResource;
import org.springframework.xd.rest.domain.JobDefinitionResource;
import org.springframework.xd.rest.domain.JobExecutionInfoResource;
import org.springframework.xd.rest.domain.JobInstanceInfoResource;
import org.springframework.xd.rest.domain.ModuleDefinitionResource;
import org.springframework.xd.rest.domain.ModuleMetadataResource;
import org.springframework.xd.rest.domain.StreamDefinitionResource;
import org.springframework.xd.rest.domain.XDRuntime;
import org.springframework.xd.rest.domain.meta.VersionResource;
import org.springframework.xd.rest.domain.metrics.AggregateCountsResource;
import org.springframework.xd.rest.domain.metrics.CounterResource;
import org.springframework.xd.rest.domain.metrics.FieldValueCounterResource;
import org.springframework.xd.rest.domain.metrics.GaugeResource;
import org.springframework.xd.rest.domain.metrics.RichGaugeResource;
import org.springframework.xd.rest.domain.security.SecurityInfoResource;

/**
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
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
		xdRuntime.add(entityLinks.linkFor(SecurityInfoResource.class).withRel("security/info"));
		xdRuntime.add(entityLinks.linkFor(ModuleMetadataResource.class).withRel("runtime/modules"));
		xdRuntime.add(entityLinks.linkFor(DetailedContainerResource.class).withRel("runtime/containers"));

		xdRuntime.add(entityLinks.linkFor(DetailedJobInfoResource.class).withRel("jobs/configurations"));
		xdRuntime.add(entityLinks.linkFor(JobExecutionInfoResource.class).withRel("jobs/executions"));
		xdRuntime.add(entityLinks.linkFor(JobInstanceInfoResource.class).withRel("jobs/instances"));

		xdRuntime.add(entityLinks.linkFor(VersionResource.class).withRel("meta/version"));

		// Maybe https://github.com/spring-projects/spring-hateoas/issues/169 will help eventually
		TemplateVariable start = new TemplateVariable("start", VariableType.REQUEST_PARAM);
		TemplateVariable lod = new TemplateVariable("detailLevel", VariableType.REQUEST_PARAM_CONTINUED);
		TemplateVariables vars = new TemplateVariables(start, lod);
		for (CompletionKind k : CompletionKind.values()) {
			Object mi = ControllerLinkBuilder.methodOn(CompletionsController.class).completions(k, "", 42);
			Link link = ControllerLinkBuilder.linkTo(mi).withRel(String.format("completions/%s", k));
			String href = link.getHref().substring(0, link.getHref().lastIndexOf('?'));
			UriTemplate template = new UriTemplate(href, vars);
			Link copy = new Link(template, link.getRel());

			xdRuntime.add(copy);
		}


		xdRuntime.add(entityLinks.linkFor(CounterResource.class).withRel("counters"));
		xdRuntime.add(entityLinks.linkFor(FieldValueCounterResource.class).withRel("field-value-counters"));
		xdRuntime.add(entityLinks.linkFor(AggregateCountsResource.class).withRel("aggregate-counters"));
		xdRuntime.add(entityLinks.linkFor(GaugeResource.class).withRel("gauges"));
		xdRuntime.add(entityLinks.linkFor(RichGaugeResource.class).withRel("rich-gauges"));
		return xdRuntime;
	}
}
