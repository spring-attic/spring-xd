/*
 * Copyright 2015 the original author or authors.
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
 *
 */

package org.springframework.xd.dirt.module;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.module.ModuleType.processor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.FileCopyUtils;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.SimpleModuleDefinition;

public class SynchronizingModuleRegistryTests {

	@Rule
	public TemporaryFolder sourceRoot = new TemporaryFolder();

	@Rule
	public TemporaryFolder targetARoot = new TemporaryFolder();

	@Rule
	public TemporaryFolder targetBRoot = new TemporaryFolder();

	private WritableResourceModuleRegistry sourceRegistry1;

	// A "source" registry that will point to the same FS (simulates shared FS)
	private WritableResourceModuleRegistry sourceRegistry2;

	// Two target registries using different FS (simulates two different nodes)
	private WritableResourceModuleRegistry targetRegistry1;
	private WritableResourceModuleRegistry targetRegistry2;


	private SynchronizingModuleRegistry synch1;

	private SynchronizingModuleRegistry synch2;

	private ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	@Before
	public void setup() throws Exception {
		sourceRegistry1 = new WritableResourceModuleRegistry(sourceRoot.getRoot().toURI().toString());
		sourceRegistry2 = new WritableResourceModuleRegistry(sourceRoot.getRoot().toURI().toString());

		targetRegistry1 = new WritableResourceModuleRegistry(targetARoot.getRoot().toURI().toString());
		targetRegistry2 = new WritableResourceModuleRegistry(targetBRoot.getRoot().toURI().toString());

		sourceRegistry1.afterPropertiesSet();
		sourceRegistry2.afterPropertiesSet();
		targetRegistry1.afterPropertiesSet();
		targetRegistry2.afterPropertiesSet();


		synch1 = new SynchronizingModuleRegistry(sourceRegistry1, targetRegistry1);
		synch2 = new SynchronizingModuleRegistry(sourceRegistry2, targetRegistry2);
	}

	@Test
	public void testStaleBecauseOfDifferentContentsFindDefinition() {

		targetRegistry1.registerNew(new UploadedModuleDefinition("foo", processor, "hello".getBytes()));
		synch1.registerNew(new UploadedModuleDefinition("foo", processor, "world".getBytes()));

		ModuleDefinition result = synch1.findDefinition("foo", processor);
		assertThat(result, pointsToContentsThat(equalTo("world")));
	}

	@Test
	public void testFindDefinitionWithInexistentTarget() {

		sourceRegistry1.registerNew(new UploadedModuleDefinition("foo", processor, "world".getBytes()));

		ModuleDefinition result = synch1.findDefinition("foo", processor);
		assertThat(result, pointsToContentsThat(equalTo("world")));
	}

	@Test
	public void testNotFoundGoesThrough() {
		assertThat(synch1.findDefinition("idontexist", processor), is(nullValue()));
	}

	@Test
	public void registerOnOneIsVisibleOnTwo() {
		synch1.registerNew(new UploadedModuleDefinition("fizz", processor, "bonjour".getBytes()));

		ModuleDefinition result = synch2.findDefinition("fizz", processor);
		assertThat(result, pointsToContentsThat(equalTo("bonjour")));
	}

	@Test
	public void deletionViaOneIsReflectedOnTwo_SingleFind() {
		UploadedModuleDefinition def = new UploadedModuleDefinition("fizz", processor, "bonjour".getBytes());
		synch1.registerNew(def);
		ModuleDefinition result = synch2.findDefinition("fizz", processor);
		assertThat(result, pointsToContentsThat(equalTo("bonjour")));
		synch1.delete(def);

		result = synch2.findDefinition("fizz", processor);
		assertThat(result, Matchers.nullValue());

	}

	@Test
	public void deletionViaOneIsReflectedOnTwo_MultiFind() {
		UploadedModuleDefinition def = new UploadedModuleDefinition("fizz", processor, "bonjour".getBytes());
		synch1.registerNew(def);
		List<ModuleDefinition> definitions = synch2.findDefinitions(processor);
		assertThat(definitions, contains(pointsToContentsThat(equalTo("bonjour"))));
		synch1.delete(def);

		definitions = synch2.findDefinitions(processor);
		assertThat(definitions, not(contains(pointsToContentsThat(equalTo("bonjour")))));

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
