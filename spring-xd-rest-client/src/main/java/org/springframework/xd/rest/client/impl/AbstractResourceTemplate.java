package org.springframework.xd.rest.client.impl;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.xd.rest.client.ResourceOperations;
import org.springframework.xd.rest.domain.support.DeploymentPropertiesFormat;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

abstract class AbstractResourceTemplate extends AbstractTemplate implements ResourceOperations {

    private final String adminUri;

    private final String password;

    private final String resourceType;

    private final String username;

    private final String vhost;

    public AbstractResourceTemplate(AbstractTemplate other, String adminUri, String password, String resourceType,
                                    String username, String vhost) {
        super(other);
        this.adminUri = adminUri;
        this.password = password;
        this.resourceType = resourceType;
        this.username = username;
        this.vhost = vhost;
    }

    public void destroy(String name) {
        // TODO: discover link by some other means (search by exact name on
        // resources??)
        String uriTemplate = resources.get(this.resourceType + "/definitions").toString() + "/{name}";
        restTemplate.delete(uriTemplate, Collections.singletonMap("name", name));
    }

    public void deploy(String name, Map<String, String> properties) {
        // TODO: discover link by some other means (search by exact name on
        // resources??)
        String uriTemplate = resources.get(this.resourceType + "/deployments").toString() + "/{name}";
        MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
        values.add("properties", DeploymentPropertiesFormat.formatDeploymentProperties(properties));
        //TODO: Do we need ResourceDeploymentResource?
        restTemplate.postForObject(uriTemplate, values, Object.class, name);
    }

    public void undeploy(String name) {
        // TODO: discover link by some other means (search by exact name on
        // /resources??)
        String uriTemplate = resources.get(this.resourceType + "/deployments").toString() + "/{name}";
        restTemplate.delete(uriTemplate, name);
    }

    public void undeployAll() {
        restTemplate.delete(resources.get(this.resourceType + "/deployments").expand());
    }

    public void destroyAll() {
        restTemplate.delete(resources.get(this.resourceType + "/definitions").expand());
    }

    public void cleanBusResources(String name) {
        String rootUri = resources.get(this.resourceType + "/clean/rabbit").toString() + "/{name}";
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(rootUri);

        if (this.adminUri != null) {
            builder.queryParam("adminUri", this.adminUri);
        }

        if (this.password != null) {
            builder.queryParam("pw", this.password);
        }

        if (this.username != null) {
            builder.queryParam("user", this.username);
        }

        if (this.vhost != null) {
            builder.queryParam("vhost", this.vhost);
        }

        URI uri = builder.buildAndExpand(Collections.singletonMap("name", name)).toUri();
        restTemplate.delete(uri);
    }
}
