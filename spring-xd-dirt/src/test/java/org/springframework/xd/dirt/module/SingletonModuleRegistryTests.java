/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.module;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.*;
import static org.springframework.xd.dirt.module.ModuleDefinitionMatchers.module;
import static org.springframework.xd.module.ModuleType.processor;
import static org.springframework.xd.module.ModuleType.sink;
import static org.springframework.xd.module.ModuleType.source;

import org.junit.Test;

import org.springframework.xd.dirt.test.SingletonModuleRegistry;


/**
 * Tests for {@link SingletonModuleRegistry}.
 *
 * @author Eric Bottard
 * @author David Turanski
 */
public class SingletonModuleRegistryTests {

	@Test
	public void testWhenMatching() {
		SingletonModuleRegistry mr = new SingletonModuleRegistry(processor, "foo");

		assertThat(mr.findDefinition("foo", processor), module("foo", processor));
		assertThat(mr.findDefinitions("foo"), contains(module("foo", processor)));
		assertThat(mr.findDefinitions(processor), contains(module("foo", processor)));
		assertThat(mr.findDefinitions(), contains(module("foo", processor)));

	}

	@Test
	public void testWhenNotMatching() {
		SingletonModuleRegistry mr = new SingletonModuleRegistry(processor, "foo");

		assertThat(mr.findDefinition("bar", processor), not(module("foo", processor)));
		assertThat(mr.findDefinitions("foo"), not(contains(module("foo", source))));
		assertThat(mr.findDefinitions(sink), not(contains(module("foo", processor))));
		assertThat(mr.findDefinitions(), not(contains(module("bar", processor))));

	}

}
