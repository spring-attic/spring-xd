package org.springframework.xd.integration.fixtures;

/**
 * Created by mpollack on 12/11/14.
 */
public class ModuleFixture {


    private ContainerResolver containerResolver;

    /**
     * Set the container resolver to back sources that depend on runtime container information, e.g. httpSource
     * @param containerResolver resolves stream and module names to runtime host and ports
     */
    public void setContainerResolver(ContainerResolver containerResolver) {
        this.containerResolver = containerResolver;
    }

    /**
     * Get the container resolver
     * @return container resolver
     */
    public ContainerResolver getContainerResolver() {
        return containerResolver;
    }
}
