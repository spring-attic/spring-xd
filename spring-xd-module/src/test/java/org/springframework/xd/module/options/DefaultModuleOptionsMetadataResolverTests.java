/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.module.options;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.xd.module.ModuleType.*;
import static org.springframework.xd.module.options.ModuleOptionMatchers.*;

import org.junit.Test;

import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDefinitions;


/**
 * Tests for {@link DefaultModuleOptionsMetadataResolver}.
 *
 * @author Eric Bottard
 */
public class DefaultModuleOptionsMetadataResolverTests {

	private DefaultModuleOptionsMetadataResolver metadataResolver = new DefaultModuleOptionsMetadataResolver();

	@Test
	@SuppressWarnings("unchecked")
	public void testPojoOptionsConstruction() {
		String resource = "classpath:/DefaultModuleOptionsMetadataResolverTests-modules/source/module1";
		ModuleDefinition definition = ModuleDefinitions.simple("module1", source, resource);
		ModuleOptionsMetadata metadata = metadataResolver.resolve(definition);
		assertThat(
				metadata,
				containsInAnyOrder(moduleOptionNamed("bar"), moduleOptionNamed("foo")));

	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMixin() {
		String resource =
				"classpath:/DefaultModuleOptionsMetadataResolverTests-modules/source/module2";
		ModuleDefinition definition = ModuleDefinitions.simple("module2", source, resource);
		ModuleOptionsMetadata metadata = metadataResolver.resolve(definition);
		assertThat(
				metadata,
				containsInAnyOrder(moduleOptionNamed("bar"), moduleOptionNamed("fizz"), moduleOptionNamed("foo"),
						moduleOptionNamed("optionDefinedHere")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMixinOverlap() {
		String resource = "classpath:/DefaultModuleOptionsMetadataResolverTests-modules/source/module3";
		ModuleDefinition definition = ModuleDefinitions.simple("module3", source, resource);
		metadataResolver.resolve(definition);
	}
}
