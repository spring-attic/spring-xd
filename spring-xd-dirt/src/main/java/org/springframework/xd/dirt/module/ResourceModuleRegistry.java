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

package org.springframework.xd.dirt.module;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.core.RuntimeIOException;
import org.springframework.xd.module.ModuleType;

/**
 * {@link Resource} based implementation of {@link ModuleRegistry} that supports two kinds of modules:
 * <ul>
 * <li>the "simple" case is a sole xml file, located in a "directory" named after the module type, <i>e.g.</i>
 * {@code source/time.xml}</li>
 * <li>the "enhanced" case is made up of a directory, where the application context file lives in a config sub-directory
 * <i>e.g.</i> {@code source/time/config/time.xml} and extra classpath is loaded from jars in a lib subdirectory
 * <i>e.g.</i> {@code source/time/lib/*.jar}</li>
 * </ul>
 * 
 * @author Mark Fisher
 * @author Glenn Renfro
 * @author Eric Bottard
 */
public class ResourceModuleRegistry extends AbstractModuleRegistry implements ResourceLoaderAware {


	private final String root;

	private final boolean active;

	private ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	public ResourceModuleRegistry(String root) {
		this.active = (StringUtils.hasText(root)) ? true : false;
		this.root = StringUtils.trimTrailingCharacter(root, '/');
	}

	@Override
	protected Resource locateApplicationContext(String name, ModuleType type) {
		try {
			String rootUri = rootForType(type);
			// Try all locations that match in case there are multiple
			Resource[] resources = resolver.getResources(rootUri + enhancedLocation(name));
			for (Resource enhanced : resources) {
				if (enhanced.exists()) {
					return enhanced;
				}
			}
			resources = resolver.getResources(rootUri + simpleLocation(name));
			for (Resource simple : resources) {
				if (simple.exists()) {
					return simple;
				}
			}
			return null;
		}
		catch (IOException e) {
			throw new RuntimeIOException(
					String.format("An error occured trying to locate context for module %s:%s", type, name), e);
		}
	}

	private String rootForType(ModuleType type) throws IOException {
		// Mind the trailing "/"
		return root + "/" + type.name() + "/";
	}

	/**
	 * Return the location (relative to {@link #rootForType(ModuleType) typed root}) where an "enhanced" application
	 * context is expected to be found.
	 */
	private String enhancedLocation(String name) {
		return String.format("%s/config/%s.xml", name, name);
	}

	/**
	 * Return the location (relative to {@link #rootForType(ModuleType) typed root}) where a "simple" application
	 * context is expected to be found.
	 */
	private String simpleLocation(String name) {
		return String.format("%s.xml", name);
	}

	@Override
	protected URL[] maybeLocateClasspath(Resource resource, String name, ModuleType type) {
		try {
			URL resourceLocation = resource.getURL();
			if (resourceLocation.toString().endsWith(enhancedLocation(name))) {
				// Can't use Resource.makeRelative here, as some implementations
				// will fail because they try to access the resulting path
				Resource[] jarsResources = resolver.getResources(StringUtils.cleanPath(resource.getURI().toString()
						+ "/../../lib/*.jar"));
				URL[] result = new URL[jarsResources.length];
				for (int i = 0; i < jarsResources.length; i++) {
					result[i] = jarsResources[i].getURL();
				}
				return result;
			}
		}
		catch (IOException e) {
			throw new RuntimeIOException(
					String.format("An error occured trying to compute the classpath for module with root=%s, %s: %s",
							root, type, name), e);
		}
		return null;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		Assert.isTrue(resourceLoader instanceof ResourcePatternResolver,
				"resourceLoader must be a ResourcePatternResolver");
		resolver = (ResourcePatternResolver) resourceLoader;
	}

	@Override
	protected List<Resource> locateApplicationContexts(ModuleType type) {
		try {
			List<Resource> result = new ArrayList<Resource>();
			String typedRootAsURI = rootForType(type);

			if (!typedRootAsURI.contains("*")) {
				Resource[] rootResources = resolver.getResources(typedRootAsURI);
				if (rootResources.length > 0 && !rootResources[0].exists()) {
					return result;
				}
			}

			String enhancedGlob = typedRootAsURI + enhancedLocation("*");
			Resource[] enhancedCandidates = resolver.getResources(enhancedGlob);
			for (Resource candidate : enhancedCandidates) {
				result.add(candidate);
			}

			String simpleGlob = typedRootAsURI + simpleLocation("*");
			Resource[] simpleCandidates = resolver.getResources(simpleGlob);
			for (Resource candidate : simpleCandidates) {
				result.add(candidate);
			}

			return result;
		}
		catch (IOException e) {
			throw new RuntimeIOException(
					String.format("An error occured trying to locate contexts for modules of type %s", type), e);
		}
	}

	@Override
	protected String inferModuleName(Resource resource) {
		return resource.getFilename().substring(0,
				resource.getFilename().lastIndexOf('.'));
	}

	@Override
	public boolean isActive() {
		return this.active;
	}

}
