/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.integration.fixtures;

/**
 * Establishes the base behavior for Acceptance Test Module Factories.
 *
 *  @author Mark Pollack
 *  @author Glenn Renfro
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
