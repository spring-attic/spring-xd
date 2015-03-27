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
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.module.SimpleModuleDefinition;

/**
 * @author Eric Bottard
 * @author David Turanski
 */
public class ModuleUtils {

	public static final String DEFAULT_EXTRA_LIBS_JAR = "/lib/*.jar";

	public static final String DEFAULT_EXTRA_LIBS_ZIP = "/lib/*.zip";

	/**
	 * Used to resolve the module 'location'. Always a file: location at the time of writing.
	 */
	private static final PathMatchingResourcePatternResolver simpleResourceResolver = new PathMatchingResourcePatternResolver();

	public static ClassLoader createModuleClassLoader(Resource moduleLocation, ClassLoader parent) {
		return createModuleClassLoader(moduleLocation, parent, true);
	}



	/**
	 * Create a classloader for a given module, possibly taking into account any extra libraries as set in the
	 * {@code module.classloader} properties setting. The default for this option if not set is to add all jars (and zips)
	 * found in the inner "lib/" folder to the module classpath.
	 *
	 * <p></p>
	 */
	public static ClassLoader createModuleClassLoader(Resource moduleLocation, ClassLoader parent,
			boolean includeExtraLibraries) {
		try {
			File moduleFile = moduleLocation.getFile();
			Archive moduleArchive = moduleFile.isDirectory() ? new ExplodedArchive(moduleFile) : new JarFileArchive
					(moduleFile);

			List<URL> urls = new ArrayList<URL>();
			if (includeExtraLibraries) {
				String[] patterns = new String[] {DEFAULT_EXTRA_LIBS_JAR, DEFAULT_EXTRA_LIBS_ZIP};

				ResourcePatternResolver resolver = new ArchiveResourceLoader(moduleArchive);

				for (String pattern : patterns) {
					for (Resource jar : resolver.getResources(pattern)) {
						urls.add(jar.getURL());
					}
				}
			}

			// Add the module archive itself
			urls.add(moduleArchive.getUrl());
			return new ParentLastURLClassLoader(urls.toArray(new URL[urls.size()]), parent);
		}
		catch (IOException e) {
			throw new RuntimeException("Exception creating module classloader for " + moduleLocation, e);
		}
	}

	/**
	 * Return a resource that can be used to load the module '.properties' file (containing <i>e.g.</i> information
	 * about module options, or null if no such file exists.
	 */
	public static Resource modulePropertiesFile(SimpleModuleDefinition definition) {
		return ModuleUtils.locateModuleResource(definition, ".properties");
	}

	/**
	 * Locate the module '.properties' file and load it as a {@link java.util.Properties} object.
	 * @return {@code null} if no properties file exists
	 */
	public static Properties loadModuleProperties(SimpleModuleDefinition moduleDefinition) {
		Resource resource = modulePropertiesFile(moduleDefinition);
		if (resource == null) {
			return null;
		}
		Properties properties = new Properties();
		try (InputStream inputStream = resource.getInputStream()) {
			properties.load(inputStream);
			return properties;
		}
		catch (IOException e) {
			throw new RuntimeException(String.format("Unable to read module properties for %s:%s",
					moduleDefinition.getName(), moduleDefinition.getType()), e);
		}
	}

	/**
	 * Return an expected module resource given a file extension. Will throw an exception if more than one such
	 * resource exists. The resource is searched using an insulated module ClassLoader that only knows about the flat
	 * contents of the module archive (does not search any parent classloader, nor any additional module library).
	 */
	public static Resource locateModuleResource(SimpleModuleDefinition definition, String extension) {

		Resource moduleLocation = simpleResourceResolver.getResource(definition.getLocation());
		Assert.isTrue(moduleLocation.exists(), "module resource " + definition.getLocation() + " does not exist");

		String ext = extension.startsWith(".") ? extension : "." + extension;

		try (
			URLClassLoader insulatedClassLoader = new ParentLastURLClassLoader(new URL[]{moduleLocation.getURL()}, NullClassLoader.NO_PARENT, true);
		) {
			PathMatchingResourcePatternResolver moduleResolver = new PathMatchingResourcePatternResolver(insulatedClassLoader);

			try {
				Resource[] resources = moduleResolver.getResources("classpath:/config/*" + ext);
				if (resources.length > 1) {
					throw new IllegalStateException("Multiple top level module resources found :" + StringUtils
							.arrayToCommaDelimitedString(resources));
				}
				else if (resources.length == 1) {
					return resources[0];
				}
			}
			catch (IOException e) {
				return null;
			}
		} catch (IOException e) {
			throw new RuntimeException("Exception creating module classloader for " + moduleLocation, e);
		}
		return null;
	}
}
