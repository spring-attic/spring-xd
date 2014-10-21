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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.xd.module.SimpleModuleDefinition;

/**
 * @author Eric Bottard
 */
public class ModuleUtils {

	public static URL[] determineClassPath(SimpleModuleDefinition definition, ResourcePatternResolver resourceLoader) {
		try {
			Resource[] jars = resourceLoader.getResources(definition.getLocation() + "/lib/*.jar");
			List<URL> result = new ArrayList<URL>(jars.length);
			result.add(resourceLoader.getResource(definition.getLocation()).getURL());
			for (Resource jar : jars) {
				result.add(jar.getURL());
			}
			return result.toArray(new URL[jars.length]);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static Resource moduleResourceFile(SimpleModuleDefinition moduleDefinition, ResourceLoader resourceLoader) {
		for (String extension : new String[] {".xml", ".groovy"}) {
			for (String subpath : new String[] {"/", "/config/"}) {
				Resource resource = resourceLoader.getResource(moduleDefinition.getLocation() + subpath + moduleDefinition.getName() + extension);
				if (resource.exists() && resource.isReadable()) {
					return resource;
				}
			}
		}
		return null;

	}

}
