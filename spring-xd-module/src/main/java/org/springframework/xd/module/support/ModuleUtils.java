/*
 * Copyright 2015 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.module.support;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.boot.loader.util.AsciiBytes;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.module.SimpleModuleDefinition;

/**
 * @author Eric Bottard
 * @author David Turanski
 */
public class ModuleUtils {

	private static final String LIB = "lib/";

	public static final String DOT_JAR = ".jar";

	public static final String DOT_ZIP = ".zip";

	public static ClassLoader createModuleClassLoader(Resource moduleLocation, ClassLoader parent) {
		return createModuleClassLoader(moduleLocation, parent, true);
	}

	public static ClassLoader createModuleClassLoader(Resource moduleLocation, ClassLoader parent,
			boolean includeNestedJars) {
		try {
			File moduleFile = moduleLocation.getFile();
			Archive moduleArchive = moduleFile.isDirectory() ? new ExplodedArchive(moduleFile) : new JarFileArchive
					(moduleFile);

			List<Archive> nestedArchives = new ArrayList<Archive>();
			if (includeNestedJars) {
				nestedArchives = moduleArchive.getNestedArchives(new Archive.EntryFilter() {
					@Override
					public boolean matches(Archive.Entry entry) {
						String name = entry.getName().toString().toLowerCase();
						return !entry.isDirectory() && name.startsWith(LIB) && 
								(name.endsWith(DOT_JAR) || name.endsWith(DOT_ZIP));
					}
				});
			}

			URL[] urls = new URL[nestedArchives.size() + 1];
			int i = 0;
			for (Archive nested : nestedArchives) {
				urls[i++] = nested.getUrl();
			}

			urls[i] = moduleArchive.getUrl();
			return new ParentLastURLClassLoader(urls, parent);
		}
		catch (IOException e) {
			throw new RuntimeException("Exception creating module classloader for " + moduleLocation, e);
		}
	}

	/**
	 * Return a resource that can be used to load the module '.properties' file (containing <i>e.g.</i> information
	 * about module options, or null if no such file exists.
	 */
	public static Resource modulePropertiesFile(SimpleModuleDefinition definition, ClassLoader moduleClassLoader) {
		return ModuleUtils.locateModuleResource(definition, moduleClassLoader, ".properties");
	}

	/**
	 * Return an expected module resource given a file extension. Will throw an exception if more than one such
	 * resource exists.
	 */
	public static Resource locateModuleResource(SimpleModuleDefinition definition, ClassLoader moduleClassLoader,
			String extension) {

		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(moduleClassLoader);
		Resource moduleLocation = resolver.getResource(definition.getLocation());
		Assert.isTrue(moduleLocation.exists(), "module resource " + definition.getLocation() + " does not exist");
		ClassLoader parentClassloader = moduleClassLoader == null? null : moduleClassLoader.getParent();
		PathMatchingResourcePatternResolver moduleResolver = new PathMatchingResourcePatternResolver
				(createModuleClassLoader(moduleLocation, parentClassloader, false));

		Resource result = null;
		String ext = extension.startsWith(".") ? extension : "." + extension;
		try {
			Resource[] resources = moduleResolver.getResources("classpath:/config/*" + ext);
			if (resources.length > 1) {
				throw new IllegalStateException("Multiple top level module resources found :" + StringUtils
						.arrayToCommaDelimitedString(resources));
			}
			else if (resources.length == 1) {
				result = resources[0];
			}
		}
		catch (IOException e) {
			return null;
		}

		return result;
	}
}
