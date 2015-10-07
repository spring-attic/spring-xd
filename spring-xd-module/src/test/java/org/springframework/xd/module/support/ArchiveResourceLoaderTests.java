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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.core.io.Resource;

/**
 * Tests for ArchiveResourceLoader and NestedArchiveResource.
 *
 * @author Eric Bottard
 */
public class ArchiveResourceLoaderTests {

	private ArchiveResourceLoader loader;

	private File archive;

	@Before
	public void setUp() {
		archive = new File("src/test/resources/" + ArchiveResourceLoaderTests.class.getPackage().getName().replace('.', '/') + "/MyArchive");
		Archive root = new ExplodedArchive(archive);

		loader = new ArchiveResourceLoader(root);

	}

	@Test
	public void testInexistentNestedPath() {

		Resource notFound = loader.getResource("/foo/bar.jar");
		assertThat(notFound.exists(), equalTo(false));
		assertThat(notFound.getFilename(), equalTo("bar.jar"));
		try {
			notFound.getFile();
			fail();
		}
		catch (IOException expected) {

		}
		try {
			notFound.getURI();
			fail();
		}
		catch (IOException expected) {

		}
		try {
			notFound.getURL();
			fail();
		}
		catch (IOException expected) {

		}
		try {
			notFound.getInputStream();
			fail();
		}
		catch (IOException expected) {

		}
	}

	@Test
	public void testGetResourceWithValidPath() throws Exception {
		Resource nested = loader.getResource("/lib/foo.jar");
		assertThat(nested.exists(), equalTo(true));
		assertThat(nested.getFilename(), equalTo("foo.jar"));
		assertThat(nested.getFile(), notNullValue());
		assertThat(nested.getURL(), notNullValue());
		assertThat(nested.getURI(), notNullValue());
	}

	@Test
	public void testGetResources() throws IOException{
		Resource[] resources = loader.getResources("/lib/*.jar");
		assertThat(resources.length, equalTo(2));
	}
}
