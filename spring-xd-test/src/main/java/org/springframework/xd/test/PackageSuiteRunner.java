/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.xd.test;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ClassUtils;


/**
 * A {@link Suite} to run all concrete classes ending in Tests in the same package.
 * 
 * @author David Turanski
 * 
 */
public class PackageSuiteRunner extends Suite {

	/**
	 * 
	 */
	private static final String TEST_CLASSNAME_SUFFIX = "Tests";

	/**
	 * @param klass
	 * @param builder
	 * @throws InitializationError
	 */
	public PackageSuiteRunner(Class<?> klass, RunnerBuilder builder) throws InitializationError {
		super(builder, klass, getClassesInPackage(klass));
	}

	/**
	 * @return selected classes in the current package
	 */
	private static Class<?>[] getClassesInPackage(Class<?> klass) {
		String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "/" +
				klass.getPackage().getName().replaceAll("\\.", "/") + "/*" + TEST_CLASSNAME_SUFFIX + ".class";
		ArrayList<Class<?>> results = new ArrayList<Class<?>>();
		ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
		try {
			Resource[] resources = resourcePatternResolver.getResources(packageSearchPath);
			if (resources != null) {
				for (Resource resource : resources) {
					String unqualifiedClassName = resource.getFilename().replace(".class", "");
					String className = klass.getPackage().getName() + "." + unqualifiedClassName;
					Class<?> candidate = ClassUtils.resolveClassName(className, klass.getClassLoader());
					if (!Modifier.isAbstract(candidate.getModifiers())) {
						results.add(candidate);
					}

				}
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		return results.toArray(new Class<?>[results.size()]);
	}

}
