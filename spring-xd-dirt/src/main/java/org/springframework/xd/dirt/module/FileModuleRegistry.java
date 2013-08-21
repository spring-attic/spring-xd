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

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;

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
public class FileModuleRegistry extends AbstractModuleRegistry implements ApplicationContextAware {

	private final File directory;

	private ResourcePatternResolver resolver;

	public FileModuleRegistry(String directory) {
		File f = new File(directory);
		Assert.isTrue(f.isDirectory(), "not a directory: " + f.getAbsolutePath());
		this.directory = f;
	}

	@Override
	protected Resource locateApplicationContext(String name, String type) {
		File typedDir = new File(directory, type);

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
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		resolver = applicationContext;
	}

}
