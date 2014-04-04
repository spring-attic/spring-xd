/*
 * Copyright 2013-2014 the original author or authors.
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

import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.module.options.HierarchicalCompositeModuleOptionsMetadataTests.makeKey;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.validation.BindException;


/**
 * 
 * @author Eric Bottard
 */
public class PrefixNarrowingModuleOptionsTests {


	@Test
	public void testProfileActivation() throws BindException {
		ModuleOptionsMetadata pojo1 = new PojoModuleOptionsMetadata(BackingPojo.class);
		ModuleOptionsMetadata pojo2 = new PojoModuleOptionsMetadata(OtherBackingPojo.class);

		Map<String, ModuleOptionsMetadata> hierarchy = new HashMap<String, ModuleOptionsMetadata>();
		hierarchy.put("pojo1", pojo1);
		hierarchy.put("pojo2", pojo2);


		HierarchicalCompositeModuleOptionsMetadata metadata = new HierarchicalCompositeModuleOptionsMetadata(hierarchy);
		Map<String, String> values = new HashMap<String, String>();
		values.put(makeKey("pojo1", "foo"), "value_of_foo");
		values.put(makeKey("pojo1", "bar"), "59");
		values.put(makeKey("pojo2", "fizz"), "value_of_fizz");
		ModuleOptions interpolated = metadata.interpolate(values);

		PrefixNarrowingModuleOptions moduleOptions = new PrefixNarrowingModuleOptions(interpolated, "pojo1");
		assertThat(moduleOptions.profilesToActivate(), hasItemInArray("high-profile"));


	}
}
