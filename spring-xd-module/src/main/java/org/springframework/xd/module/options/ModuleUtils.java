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

package org.springframework.xd.module.options;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.module.SimpleModuleDefinition;
import org.springframework.xd.module.options.ModuleOptions;
import org.springframework.xd.module.support.ArchiveResourceLoader;
import org.springframework.xd.module.support.NullClassLoader;
import org.springframework.xd.module.support.ParentLastURLClassLoader;

/**
 * Contains utility methods for accessing a module's properties and dealing with ClassLoaders.
 *
 * @author Eric Bottard
 * @author David Turanski
 */
public class ModuleUtils {

	private static final List<String> DEFAULT_EXTRA_LIBS = Arrays.asList("/lib/*.jar", "/lib/*.zip");

	private static final String MODULE_CLASSPATH_KEY = "module.classpath";

	/**
	 * Used to resolve the module 'location'. Always a file: location at the time of writing.
	 */
	private static final PathMatchingResourcePatternResolver simpleResourceResolver = new PathMatchingResourcePatternResolver();

	/**
	 * Create a ClassLoader suitable for running a module. Extra libraries can come from paths that are derived from
	 * module options. Any path that starts with a slash will be assumed to be an in-Archive entry, whereas any other
	 * path (including those starting with a protocol) will be dealt with by a classical resource pattern resolver.
	 */
	public static ClassLoader createModuleRuntimeClassLoader(SimpleModuleDefinition definition, ModuleOptions moduleOptions, ClassLoader parent) {
		Resource moduleLocation = simpleResourceResolver.getResource(definition.getLocation());

		Properties moduleProperties = loadModuleProperties(definition);
		moduleProperties = moduleProperties == null ? new Properties() : moduleProperties;
		String extraLibsCSV = moduleProperties.getProperty(MODULE_CLASSPATH_KEY, StringUtils.collectionToCommaDelimitedString(DEFAULT_EXTRA_LIBS));
		List<String> extraLibs = new ArrayList<>();
		String[] paths = extraLibsCSV.split("\\s*,\\s*");
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(moduleOptions.asPropertySource());
		PropertyResolver placeHolderResolver = new PropertySourcesPropertyResolver(propertySources);
		for (String path : paths) {
			try {
				extraLibs.add(placeHolderResolver.resolveRequiredPlaceholders(path));
			}
			catch (IllegalArgumentException ignored) {
			}
		}

		return createModuleClassLoader(moduleLocation, parent, extraLibs);
	}

	/**
	 * Create a "simple" ClassLoader for a module, suitable for early discovery phase, before module options are known.
	 * Only the default library paths are used.
	 */
	public static ClassLoader createModuleDiscoveryClassLoader(Resource moduleLocation, ClassLoader parent) {
		return createModuleClassLoader(moduleLocation, parent, DEFAULT_EXTRA_LIBS);
	}


	private static ClassLoader createModuleClassLoader(Resource moduleLocation, ClassLoader parent,
			Iterable<String> patterns) {
		try {
			File moduleFile = moduleLocation.getFile();
			Archive moduleArchive = moduleFile.isDirectory() ? new ExplodedArchive(moduleFile) : new JarFileArchive
					(moduleFile);

			List<URL> urls = new ArrayList<>();

			ResourcePatternResolver resolver = new ArchiveResourceLoader(moduleArchive);

			for (String pattern : patterns) {
				for (Resource jar : resolver.getResources(pattern)) {
					urls.add(jar.getURL());
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

		try {
			URLClassLoader insulatedClassLoader = new ParentLastURLClassLoader(new URL[] {moduleLocation.getURL()}, NullClassLoader.NO_PARENT, true);
			PathMatchingResourcePatternResolver moduleResolver = new PathMatchingResourcePatternResolver(insulatedClassLoader);

			try {
				Resource[] resources = moduleResolver.getResources("classpath:/config/*" + ext);
				if (resources.length > 1) {
					throw new IllegalStateException("Multiple top level module resources found :" + StringUtils
							.arrayToCommaDelimitedString(resources));
				}
				else if (resources.length == 1) {
					// Convert from ClassPathResource to UrlResource, as CPR.getInputStream()
					// will apply caching, which we don't want we re-deploying. See XD-3141
					Resource resource = resources[0];
					if (resource instanceof ClassPathResource) {
						return new UrlResource(resource.getURL());
					} else {
						return resource;
					}
				}
			}
			catch (IOException e) {
				return null;
			}
		}
		catch (IOException e) {
			throw new RuntimeException("Exception creating module classloader for " + moduleLocation, e);
		}
		return null;
	}

	/**
	 * Return the resource that can be used to configure a module, or null if no such resource exists.
	 *
	 * @throws java.lang.IllegalStateException if both a .xml and .groovy file are present
	 */
	public static Resource resourceBasedConfigurationFile(SimpleModuleDefinition moduleDefinition) {
		Resource xml = ModuleUtils.locateModuleResource(moduleDefinition, ".xml");
		Resource groovy = ModuleUtils.locateModuleResource(moduleDefinition, ".groovy");
		boolean xmlExists = xml != null;
		boolean groovyExists = groovy != null;
		if (xmlExists && groovyExists) {
			throw new IllegalStateException(String.format("Found both resources '%s' and '%s' for module %s", xml,
					groovy, moduleDefinition));
		}
		else if (xmlExists) {
			return xml;
		}
		else if (groovyExists) {
			return groovy;
		}
		else {
			return null;
		}

	}
}
