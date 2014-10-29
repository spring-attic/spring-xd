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

import org.springframework.boot.loader.JarLauncher;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.boot.loader.jar.JarFile;
import org.springframework.boot.loader.util.AsciiBytes;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.xd.module.SimpleModuleDefinition;

/**
 * @author Eric Bottard
 */
public class ModuleUtils {

	private static final AsciiBytes LIB = new AsciiBytes("lib/");

	public static ClassLoader createModuleClassLoader(Resource moduleLocation, ClassLoader parent) {
		try {
			File moduleFile = moduleLocation.getFile();
			Archive moduleArchive = moduleFile.isDirectory() ? new ExplodedArchive(moduleFile) : new JarFileArchive(moduleFile);
			List<Archive> nestedArchives = moduleArchive.getNestedArchives(new Archive.EntryFilter() {
				@Override
				public boolean matches(Archive.Entry entry) {
					return !entry.isDirectory() && entry.getName().startsWith(LIB);
				}
			});
			URL[] urls = new URL[nestedArchives.size() + 1];
			int i = 0;
			for (Archive nested : nestedArchives) {
				urls[i++] = nested.getUrl();
			}
			urls[i] = moduleArchive.getUrl();
			return new ParentLastURLClassLoader(urls, parent);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Return a resource that can be used to load the module '.properties' file (containing <i>e.g.</i> information
	 * about module options, or null if no such file exists.
	 */
	public static Resource modulePropertiesFile(SimpleModuleDefinition definition, ClassLoader moduleClassLoader) {
		Resource result = new ClassPathResource("/config/" + definition.getName() + ".properties", moduleClassLoader);
		return result.exists() && result.isReadable() ? result : null;
	}

}
