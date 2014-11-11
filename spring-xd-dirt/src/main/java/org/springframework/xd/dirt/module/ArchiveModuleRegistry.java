/*
 * Copyright 2011-2014 the original author or authors.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.core.RuntimeIOException;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDefinitions;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.SimpleModuleDefinition;

/**
 * A ModuleRegistry that expects to find Spring Boot archives (either as jar file or exploded directory)
 * at the following location pattern: {@code <registry root>/<module type>/<archive named after the module>}.
 *
 * @author Eric Bottard
 */
public class ArchiveModuleRegistry implements ModuleRegistry, ResourceLoaderAware {

	/**
	 * The extension the module 'File' must have if it's not in exploded dir format.
	 */
	public static final String ARCHIVE_AS_FILE_EXTENSION = ".jar";

	private final static String[] SUFFIXES = new String[] { "", ARCHIVE_AS_FILE_EXTENSION };

	private String root;

	private ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	public ArchiveModuleRegistry(String root) {
		this.root = StringUtils.trimTrailingCharacter(root, '/');
	}

	@Override
	public ModuleDefinition findDefinition(String name, ModuleType moduleType) {
		List<ModuleDefinition> result = new ArrayList<ModuleDefinition>();
		try {
			for (String ext : SUFFIXES) {
				String location = String.format("%s/%s/%s%s", root, moduleType.name(), name, ext);
				Resource[] resources = resolver.getResources(location);
				for (Resource resource : resources) {
					fromResource(resource, result);
				}
			}
		}
		catch (IOException e) {
			throw new RuntimeIOException(String.format("An error occurred trying to locate module '%s:%s'", moduleType,
					name), e);
		}

		return result.size() == 1 ? result.iterator().next() : null;

	}

	@Override
	public List<ModuleDefinition> findDefinitions(String name) {
		List<ModuleDefinition> result = new ArrayList<ModuleDefinition>();
		try {
			for (String suffix : SUFFIXES) {
				Resource[] resources = resolver.getResources(String.format("%s/*/%s%s", root, name, suffix));
				for (Resource resource : resources) {
					fromResource(resource, result);
				}
			}
		}
		catch (IOException e) {
			return Collections.emptyList();
		}
		return result;
	}

	@Override
	public List<ModuleDefinition> findDefinitions(ModuleType type) {
		List<ModuleDefinition> result = new ArrayList<ModuleDefinition>();
		try {
			Resource[] resources = resolver.getResources(String.format("%s/%s/*", root, type.name()));
			for (Resource resource : resources) {
				fromResource(resource, result);
			}
		}
		catch (IOException e) {
			return Collections.emptyList();
		}
		return result;
	}

	@Override
	public List<ModuleDefinition> findDefinitions() {
		List<ModuleDefinition> result = new ArrayList<ModuleDefinition>();
		try {
			Resource[] resources = resolver.getResources(String.format("%s/*/*", root));
			for (Resource resource : resources) {
				fromResource(resource, result);
			}
		}
		catch (IOException e) {
			return Collections.emptyList();
		}
		return result;
	}

	private void fromResource(Resource resource, List<ModuleDefinition> holder) throws IOException {
		if (!resource.exists()) {
			return;
		}
		String filename = resource.getFile().getCanonicalFile().getName();

		boolean isDir = resource.getFile().isDirectory();
		if (!isDir && !filename.endsWith(ARCHIVE_AS_FILE_EXTENSION)) {
			return;
		}
		String name = isDir ? filename : filename.substring(0, filename.lastIndexOf(ARCHIVE_AS_FILE_EXTENSION));
		String canonicalPath = resource.getFile().getCanonicalPath();

		String fileSeparator = File.separator;
		int lastSlash = canonicalPath.lastIndexOf(fileSeparator);
		String typeAsString = canonicalPath.substring(canonicalPath.lastIndexOf(fileSeparator, lastSlash - 1) + 1,
				lastSlash);
		ModuleType type = null;
		try {
			type = ModuleType.valueOf(typeAsString);
		}
		catch (IllegalArgumentException e) {
			// Not an actual type name, skip
			return;
		}
		ModuleDefinition found = ModuleDefinitions.simple(name, type,
				"file:" + canonicalPath + (isDir ? fileSeparator : ""));

		if (holder.contains(found)) {
			SimpleModuleDefinition one = (SimpleModuleDefinition) found;
			SimpleModuleDefinition two = (SimpleModuleDefinition) holder.get(holder.indexOf(found));
			throw new IllegalStateException(String.format(
					"Duplicate module definitions for '%s:%s' found at '%s' and '%s'",
					found.getType(), found.getName(), one.getLocation(), two.getLocation()));
		}
		else {
			holder.add(found);
		}
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		Assert.isTrue(resourceLoader instanceof ResourcePatternResolver,
				"resourceLoader must be a ResourcePatternResolver");
		resolver = (ResourcePatternResolver) resourceLoader;
	}

}
