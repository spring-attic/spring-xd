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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;

/**
 * File based implementation of {@link ModuleRegistry} that supports two kinds of modules:
 * <ul>
 * <li>the "simple" case is a sole xml file, located in a directory named after the module type, <i>e.g.</i>
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
public class FileModuleRegistry extends AbstractModuleRegistry implements ResourceLoaderAware {

	private final File directory;

	private ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	public FileModuleRegistry(String directory) {
		File f = new File(directory);
		Assert.isTrue(f.isDirectory(), "not a directory: " + f.getAbsolutePath());
		this.directory = f;
	}

	@Override
	protected Resource locateApplicationContext(String name, ModuleType type) {
		File typedDir = new File(directory, type.getTypeName());
		File enhanced = new File(typedDir, name + File.separator + "config" + File.separator + name + ".xml");
		if (enhanced.exists()) {
			return new FileSystemResource(enhanced);
		}
		File file = new File(typedDir, name + ".xml");
		if (file.exists()) {
			return new FileSystemResource(file);
		}
		return null;
	}

	@Override
	protected URL[] maybeLocateClasspath(Resource resource, String name, String type) {
		try {
			URL resourceLocation = resource.getURL();
			if (resourceLocation.toString().endsWith(name + "/config/" + name + ".xml")) {
				Resource jarsPattern = resource.createRelative("../lib/*.jar");
				Resource[] jarsResources = resolver.getResources(jarsPattern.getURI().toString());
				URL[] result = new URL[jarsResources.length];
				for (int i = 0; i < jarsResources.length; i++) {
					result[i] = jarsResources[i].getURL();
				}
				return result;
			}
		}
		catch (IOException e) {
			// Can't happen as we know resource is a FileSystemResource
			throw new IllegalStateException(e);
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
	public List<ModuleDefinition> findDefinitions(ModuleType type) {
		ArrayList<ModuleDefinition> results = new ArrayList<ModuleDefinition>();
		for (Resource resource : locateApplicationContexts(type)) {
			String name = resource.getFilename().substring(0,
					resource.getFilename().lastIndexOf('.'));
			results.add(new ModuleDefinition(name, type.getTypeName(), resource, maybeLocateClasspath(resource, name,
					type.getTypeName())));
		}
		return results;
	}

	@Override
	protected List<Resource> locateApplicationContexts(ModuleType type) {
		ArrayList<Resource> resources = new ArrayList<Resource>();
		File typedDir = new File(directory, type.getTypeName());
		File[] files = typedDir.listFiles();
		if (files == null) {
			return resources;
		}

		for (File file : files) {
			if (file.isFile()) {
				String fileName = file.getName();
				int i = fileName.lastIndexOf('.');
				if (i > 0) {
					if (fileName.substring(i + 1).equals("xml")) {
						resources.add(new FileSystemResource(file));
					}
				}
			}
			else if (file.isDirectory()) {
				FileSystemResource configResource = getResourceFromConfigDir(file, type.getTypeName());
				if (configResource != null) {
					resources.add(configResource);
				}
			}
		}
		return resources;
	}

	private FileSystemResource getResourceFromConfigDir(File file, String typeName) {
		FileSystemResource result = null;
		String moduleName = file.getName();
		File moduleFile = new File(file.getPath() + "/config/" + moduleName + ".xml");
		if (moduleFile.exists()) {
			result = new FileSystemResource(moduleFile);
		}
		return result;
	}

	@Override
	public List<ModuleDefinition> findDefinitions() {
		ArrayList<ModuleDefinition> results = new ArrayList<ModuleDefinition>();
		for (ModuleType type : ModuleType.values()) {
			results.addAll(findDefinitions(type));
		}
		return results;
	}

}
