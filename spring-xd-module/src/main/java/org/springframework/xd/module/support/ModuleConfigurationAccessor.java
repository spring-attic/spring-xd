/*
 *
 *  * Copyright 2011-2014 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Centralizes access to common module configuration aspects, ensuring that the correct classloader is used, etc.
 *
 * @author Eric Bottard
 */
public class ModuleConfigurationAccessor {

	private final ClassLoader moduleClassLoader;

	public ModuleConfigurationAccessor(Resource moduleLocation, ClassLoader parent) {
		try {
			File moduleFile = moduleLocation.getFile();
			Archive moduleArchive = moduleFile.isDirectory() ? new ExplodedArchive(moduleFile) : new JarFileArchive
					(moduleFile);

			String[] patterns = new String[] {"/lib/*.jar"};

			ResourcePatternResolver resolver = new ArchiveResourceLoader(moduleArchive);

			List<URL> urls = new ArrayList<URL>();
			for (String pattern : patterns) {
				for (Resource jar : resolver.getResources(pattern)) {
					urls.add(jar.getURL());
				}
			}

			// Add the module archive itself
			urls.add(moduleArchive.getUrl());
			moduleClassLoader = new ParentLastURLClassLoader(urls.toArray(new URL[urls.size()]), parent);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
}
