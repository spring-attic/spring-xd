/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.module;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.core.RuntimeIOException;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;


/**
 * A module registry that loads definition from "archives", where the layout of an archive is as follows:
 * <ul>
 * <li>{@code spring-xd/}
 * <ul>
 * <li>{@code <type1>/}
 * <ul>
 * <li>{@code <module1>.xml}</li>
 * <li>{@code <module2>.xml}</li>
 * </ul>
 * <li>{@code <type2>/}
 * <ul>
 * <li>{@code <module1>.xml}</li>
 * <li>{@code <module2>.xml}</li>
 * </ul>
 * </ul>
 * </li>
 * <li>{@code lib/}
 * <ul>
 * <li>{@code some.jar}</li>
 * <li>{@code some.jar}</li>
 * </ul>
 * </li>
 * </ul>
 * 
 * That is, modules are sorted by {@linkplain ModuleType type} and an archive can contain several module definitions.
 * Those would all benefit from the classpath formed by the jar files located under {@code lib/}
 * 
 * <p>
 * The {@code /spring-xd} directory is configurable (even to the empty string) and serves as a precautionary measure to
 * avoid accidental lookup of any random xml file as a module definition.
 * </p>
 * 
 * <p>
 * The repository itself is configured by a {@code root} that serves as the basis of a
 * {@link PathMatchingResourcePatternResolver scan} and can itself contain wildcards. As an example,
 * {@code file:/somedir/*} would pick up all archives (dirs or actual archive files) that are in the {@code somedir}
 * directory.
 * </p>
 * 
 * @author Eric Bottard
 */
public class ResourceModuleRegistry2 implements ModuleRegistry {

	private static final String DEFAULT_DEFINITION_SUBDIRECTORY = "/spring-xd";

	private static final int NAME_CAPTURING_GROUP = 2;

	private static final int TYPE_CAPTURING_GROUP = 1;

	private final Pattern capturingPattern;

	/**
	 * Where in the module "archive" should the module definition be located. Exists to avoid accidental lookup of a
	 * random xml file as an XD module definition.
	 */
	private final String definitionSubdirectory;

	private ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	private final String root;

	public ResourceModuleRegistry2(String root) {
		this(root, DEFAULT_DEFINITION_SUBDIRECTORY);
	}

	public ResourceModuleRegistry2(String root, String definitionSubdirectory) {
		this.root = StringUtils.trimTrailingCharacter(root, '/');
		Assert.isTrue(definitionSubdirectory.length() == 0 || definitionSubdirectory.startsWith("/"),
				"definitionSubdirectory should start with a '/'");
		this.definitionSubdirectory = StringUtils.trimTrailingCharacter(definitionSubdirectory, '/');
		capturingPattern = Pattern.compile("^.*" + this.definitionSubdirectory
				+ "/(.+)/(.+)\\.xml$");
	}


	@Override
	public ModuleDefinition findDefinition(String name, ModuleType moduleType) {
		List<ModuleDefinition> maybeMany = lookupByPattern(String.format("/%s/%s.xml", moduleType.name(), name));
		return maybeMany.isEmpty() ? null : maybeMany.get(0);
	}

	@Override
	public List<ModuleDefinition> findDefinitions() {
		return lookupByPattern("/*/*.xml");
	}

	@Override
	public List<ModuleDefinition> findDefinitions(ModuleType type) {
		return lookupByPattern(String.format("/%s/*.xml", type.name()));
	}

	@Override
	public List<ModuleDefinition> findDefinitions(String name) {
		return lookupByPattern(String.format("/*/%s.xml", name));
	}

	private List<ModuleDefinition> lookupByPattern(String pattern) {
		Assert.isTrue(pattern.startsWith("/"), "Pattern should start with a '/' : " + pattern);
		List<ModuleDefinition> result = new ArrayList<>();
		try {
			Resource[] resources = resolver.getResources(root + definitionSubdirectory + pattern);
			for (Resource resource : resources) {
				if (!resource.exists()) {
					// PathMatchingResourcePatternResolver.getResources() may return a non-existent
					// resource when given a pattern-less glob
					continue;
				}
				String fullPath = resource.getURI().toString();
				Matcher matcher = capturingPattern.matcher(fullPath);
				Assert.isTrue(matcher.matches(), fullPath + " did not match " + capturingPattern);
				ModuleType type = ModuleType.valueOf(matcher.group(TYPE_CAPTURING_GROUP));
				String actualName = matcher.group(NAME_CAPTURING_GROUP);
				URL[] urls = locateJarsRelativeToResource(resource);
				result.add(new ModuleDefinition(actualName, type, resource, urls));
			}
		}
		catch (IOException e) {
			throw new RuntimeIOException("Exception while trying to find resources matching " + root + pattern, e);
		}
		return result;
	}

	private URL[] locateJarsRelativeToResource(Resource resource) throws IOException {
		StringBuilder sb = new StringBuilder(resource.getURI().toString());
		// From .../subdir/sink/foo.xml into sink/
		sb.setLength(sb.lastIndexOf("/") + 1);
		// Now move up as necessary to balance definitionSubdirectory
		int levels = StringUtils.countOccurrencesOf(definitionSubdirectory, "/");
		for (int i = 0; i < levels; i++) {
			sb.append("../");
		}
		sb.append("../lib/*.jar");
		try {
			Resource[] resources = resolver.getResources(sb.toString());
			URL[] result = new URL[resources.length];
			for (int i = 0; i < resources.length; i++) {
				result[i] = resources[i].getURL();
			}
			return result;
		}
		catch (IOException e) {
			throw new RuntimeIOException("Exception while trying to locate jars inside " + sb, e);
		}
	}

}
