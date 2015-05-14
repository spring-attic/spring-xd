package org.springframework.xd.rest.client.impl;

import org.springframework.xd.rest.domain.metrics.MetricResource;

abstract class AbstractSingleMetricTemplate<T extends MetricResource> extends AbstractMetricTemplate {

    private final String resourcesKey;

    private final Class<T> resourceType;

    public AbstractSingleMetricTemplate(AbstractTemplate other, String resourcesKey, Class<T> resourceType) {
        super(other, resourcesKey);
        this.resourcesKey = resourcesKey;
        this.resourceType = resourceType;
    }

    public T retrieve(String name) {
        String url = resources.get(this.resourcesKey).toString() + "/{name}";
        return restTemplate.getForObject(url, this.resourceType, name);
    }
}
