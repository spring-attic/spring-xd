package org.springframework.xd.integration.fixtures;

import org.springframework.util.Assert;
import org.springframework.xd.integration.util.StreamUtils;
import org.springframework.xd.rest.domain.ModuleMetadataResource;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * Resolve runtime hosts for modules.
 *
 * @author Mark Pollack
 */
public class ContainerResolver {

    private final URL adminServer;
    private final Map<String, String> containers;
    private final String defaultStreamName;


    public ContainerResolver(URL adminServer, Map<String, String> containers, String defaultStreamName) {
        this.adminServer = adminServer;
        this.containers = containers;
        Assert.hasText(defaultStreamName, "stream name can not be empty nor null");
        this.defaultStreamName = defaultStreamName;
    }

    /**
     * Gets the URL of the container for the source being tested.
     *
     * @return The URL that contains the source.
     */
    public URL getContainerUrlForSource() {
        return getContainerUrlForSource(defaultStreamName);
    }

    /**
     * Gets the URL of the container where the source was deployed using default XD Port.
     *
     * @param streamName Used to find the container that contains the source.
     * @return The URL that contains the source.
     */
    public URL getContainerUrlForSource(String streamName) {
        return getContainerHostForURL(streamName, ModuleType.source);
    }

    /**
     * Gets the URL of the container for the sink being tested.
     *
     * @return The URL that contains the sink.
     */
    public URL getContainerUrlForSink() {
        return getContainerUrlForSink(defaultStreamName);
    }

    /**
     * Gets the URL of the container where the sink was deployed using default XD Port.
     *
     * @param streamName Used to find the container that contains the sink.
     * @return The URL that contains the sink.
     */
    public URL getContainerUrlForSink(String streamName) {
        return getContainerHostForURL(streamName, ModuleType.sink);
    }

    /**
     * Gets the URL of the container where the processor was deployed
     *
     * @return The URL that contains the sink.
     */

    public URL getContainerUrlForProcessor() {
        return getContainerUrlForProcessor(defaultStreamName);
    }


    /**
     * Gets the URL of the container where the processor was deployed
     *
     * @param streamName Used to find the container that contains the processor.
     * @return The URL that contains the processor.
     */
    public URL getContainerUrlForProcessor(String streamName) {

        return getContainerHostForURL(streamName, ModuleType.processor);
    }

    /**
     * Gets the host of the container where the source was deployed
     *
     * @return The host that contains the source.
     */
    public String getContainerHostForSource() {
        return getContainerHostForSource(defaultStreamName);
    }

    /**
     * Gets the host of the container where the source was deployed
     *
     * @param streamName Used to find the container that contains the source.
     * @return The host that contains the source.
     */
    public String getContainerHostForSource(String streamName) {
        return getContainerHostForModulePrefix(streamName, ModuleType.source);
    }

    /**
     * Finds the container URL where the module is deployed with the stream name & module type
     *
     * @param streamName The name of the stream that the module is deployed
     * @param moduleType The type of module that we are seeking
     * @return the container url.
     */
    private URL getContainerHostForURL(String streamName, ModuleType moduleType) {
        URL result = null;
        try {
            result = new URL("http://"
                    + getContainerHostForModulePrefix(streamName, moduleType) + ":" + adminServer.getPort());
        }
        catch (MalformedURLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return result;
    }


    /**
     * Gets the URL of the container where the module
     *
     * @param streamName Used construct the module id prefix.
     * @return The URL that contains the module.
     */
    public String getContainerHostForModulePrefix(String streamName, ModuleType moduleType) {
        Assert.hasText(streamName, "stream name can not be empty nor null");
        String moduleIdPrefix = streamName + "." + moduleType + ".";
        Iterator<ModuleMetadataResource> resourceIter = StreamUtils.getRuntimeModules(adminServer).iterator();
        ArrayList<String> containerIds = new ArrayList<String>();
        while (resourceIter.hasNext()) {
            ModuleMetadataResource resource = resourceIter.next();
            if (resource.getModuleId().startsWith(moduleIdPrefix)) {
                containerIds.add(resource.getContainerId());
            }
        }
        Assert.isTrue(
                containerIds.size() == 1,
                "Test require that module to be deployed to only one container. It was deployed to "
                        + containerIds.size() + " containers");
        return containers.get(containerIds.get(0));
    }

}
