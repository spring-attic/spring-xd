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

package org.springframework.xd.dirt.module;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.module.ModuleType.processor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.FileCopyUtils;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.SimpleModuleDefinition;

public class RemoteResourceProxifyingModuleRegistryTests {

	@Rule
	public TemporaryFolder sourceRoot = new TemporaryFolder();

	@Rule
	public TemporaryFolder targetRoot = new TemporaryFolder();

	private SynchronizingModuleRegistry proxy;

	private WriteCapableArchiveModuleRegistry sourceRegistry;

	private WriteCapableArchiveModuleRegistry targetRegistry;

	private ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	@Before
	public void setup() {
		sourceRegistry = new WriteCapableArchiveModuleRegistry(sourceRoot.getRoot().toURI().toString());
		targetRegistry = new WriteCapableArchiveModuleRegistry(targetRoot.getRoot().toURI().toString());

		proxy = new SynchronizingModuleRegistry(sourceRegistry, targetRegistry);
	}

	@Test
	public void testStaleBecauseOfDifferentContentsFindDefinition() {

		Object foo = null;
		String bar = (String)foo;



		targetRegistry.registerNew(new UploadedModuleDefinition("foo", processor, "hello".getBytes()));
		sourceRegistry.registerNew(new UploadedModuleDefinition("foo", processor, "world".getBytes()));

		ModuleDefinition result = proxy.findDefinition("foo", processor);
		assertThat(result, pointsToContentsThat(equalTo("world")));
	}

	@Test
	public void testFindDefinitionWithInexistentTarget() {

		sourceRegistry.registerNew(new UploadedModuleDefinition("foo", processor, "world".getBytes()));

		ModuleDefinition result = proxy.findDefinition("foo", processor);
		assertThat(result, pointsToContentsThat(equalTo("world")));
	}

	@Test
	public void testNotFoundGoesThru() {
		assertThat(proxy.findDefinition("idontexist", processor), is(nullValue()));
	}

	private Matcher<ModuleDefinition> pointsToContentsThat(final Matcher<String> delegate) {
		return new DiagnosingMatcher<ModuleDefinition>() {
			@Override
			protected boolean matches(Object item, Description mismatchDescription) {
				if (item instanceof SimpleModuleDefinition) {
					SimpleModuleDefinition simpleModuleDefinition = (SimpleModuleDefinition) item;
					try {
						InputStream is = resolver.getResource(simpleModuleDefinition.getLocation()).getInputStream();
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						FileCopyUtils.copy(is, bos);
						delegate.describeMismatch(bos.toString(), mismatchDescription);
						return delegate.matches(bos.toString());
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
				else {
					mismatchDescription.appendText(" is not a " + SimpleModuleDefinition.class.getSimpleName());
					return false;
				}
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("a module definition with contents ").appendDescriptionOf(delegate);
			}
		};
	}

}