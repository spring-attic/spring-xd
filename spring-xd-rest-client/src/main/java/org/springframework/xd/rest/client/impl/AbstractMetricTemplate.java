package org.springframework.xd.rest.client.impl;

import org.springframework.hateoas.PagedResources;
import org.springframework.xd.rest.domain.metrics.MetricResource;


abstract class AbstractMetricTemplate extends AbstractTemplate {

	private final String resourcesKey;

	public AbstractMetricTemplate(AbstractTemplate other, String resourcesKey) {
		super(other);
		this.resourcesKey = resourcesKey;
	}

	public PagedResources<MetricResource> list() {
		String url = resources.get(this.resourcesKey).toString() + "?size=10000";
		return restTemplate.getForObject(url, MetricResource.Page.class);
	}

	public void delete(String name) {
		String url = resources.get(this.resourcesKey).toString() + "/{name}";
		restTemplate.delete(url, name);
	}
}
