/*
 * Copyright 2014 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.module.support;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Test;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * @author David Turanski
 */
public class ParentLastURLClassLoaderTests {
	@Test
	public void test() throws IOException {

		ResourcePatternResolver resourceLoader = new PathMatchingResourcePatternResolver();
		Resource module = resourceLoader.getResource("DefaultModuleOptionsMetadataResolverTests-modules/source/module1/");
		assertTrue(module.exists());

		URLClassLoader classLoader = new ParentLastURLClassLoader(new URL[] {module.getURL()}, getClass().getClassLoader());

		assertNotNull(classLoader.getResource("config/foo.properties"));

	}
}
