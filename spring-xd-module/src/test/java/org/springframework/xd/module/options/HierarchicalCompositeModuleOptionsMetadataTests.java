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

import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.module.options.ModuleOptionMatchers.moduleOptionNamed;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.validation.BindException;
import org.springframework.xd.module.core.CompositeModule;


/**
 * Tests for HierarchicalCompositeModuleOptionsMetadata.
 * 
 * @author Eric Bottard
 */
public class HierarchicalCompositeModuleOptionsMetadataTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();


	private HierarchicalCompositeModuleOptionsMetadata metadata;

	@Before
	public void setup() {
		ModuleOptionsMetadata pojo1 = new PojoModuleOptionsMetadata(BackingPojo.class);
		ModuleOptionsMetadata pojo2 = new PojoModuleOptionsMetadata(OtherBackingPojo.class);

		Map<String, ModuleOptionsMetadata> hierarchy = new HashMap<String, ModuleOptionsMetadata>();
		hierarchy.put("pojo1", pojo1);
		hierarchy.put("pojo2", pojo2);


		metadata = new HierarchicalCompositeModuleOptionsMetadata(hierarchy);

	}

	@Test
	public void testBindingWrongType() throws BindException {
		Map<String, String> values = new HashMap<String, String>();
		values.put(makeKey("pojo1", "foo"), "othervalue");
		values.put(makeKey("pojo1", "bar"), "17.29");

		thrown.expect(BindException.class);
		thrown.expectMessage("bar");
		thrown.expectMessage("int");

		metadata.interpolate(values);

	}

	/* default */static String makeKey(String moduleName, String key) {
		return moduleName + CompositeModule.OPTION_SEPARATOR + key;
	}

	@Test
	public void testDefaultValues() throws BindException {
		Map<String, String> values = new HashMap<String, String>();
		ModuleOptions interpolated = metadata.interpolate(values);

		EnumerablePropertySource<?> ps = interpolated.asPropertySource();
		String p1 = makeKey("pojo2", "fizz");
		String p2 = makeKey("pojo1", "foo");
		String p3 = makeKey("pojo1", "fooBar");
		assertThat(
				ps.getPropertyNames(),
				arrayContainingInAnyOrder(p1, p2, p3));
		assertThat(ps.getProperty(p1), equalTo((Object) "hello"));
		assertThat(ps.getProperty(p2), equalTo((Object) "somedefault"));
		assertThat(ps.getProperty(p3), equalTo((Object) "somedefault42"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNamesIteration() {
		assertThat(
				metadata,
				containsInAnyOrder(moduleOptionNamed(makeKey("pojo1", "bar")),
						moduleOptionNamed(makeKey("pojo2", "fizz")),
						moduleOptionNamed(makeKey("pojo1", "foo"))));
	}

	@Test
	public void testSuccessfulInterpolation() throws Exception {
		Map<String, String> values = new HashMap<String, String>();
		values.put(makeKey("pojo1", "foo"), "value_of_foo");
		values.put(makeKey("pojo1", "bar"), "59");
		values.put(makeKey("pojo2", "fizz"), "value_of_fizz");
		ModuleOptions interpolated = metadata.interpolate(values);

		EnumerablePropertySource<?> ps = interpolated.asPropertySource();
		assertThat(ps.getProperty(makeKey("pojo2", "fizz")), equalTo((Object) "value_of_fizz"));
		assertThat(ps.getProperty(makeKey("pojo1", "foo")), equalTo((Object) "value_of_foo"));
		assertThat(ps.getProperty(makeKey("pojo1", "fooBar")), equalTo((Object) "value_of_foo59"));

	}

	@Test
	public void testUnknownOption() throws BindException {
		Map<String, String> values = new HashMap<String, String>();
		values.put(makeKey("pojo1", "foo"), "othervalue");
		values.put(makeKey("pojo3", "bar"), "othervalue");
		values.put("wizz", "1729");

		thrown.expect(BindException.class);
		thrown.expectMessage("wizz");
		thrown.expectMessage(makeKey("pojo3", "bar"));

		metadata.interpolate(values);
	}

	@Test
	public void testValidation() throws BindException {
		Map<String, String> values = new HashMap<String, String>();
		values.put(makeKey("pojo1", "foo"), "othervalue");
		values.put(makeKey("pojo1", "bar"), "65536");

		thrown.expect(BindException.class);
		// Note validation message only refers to unqualified name
		thrown.expectMessage("bar");
		thrown.expectMessage("int");
		thrown.expectMessage("65536");
		thrown.expectMessage("10000");

		metadata.interpolate(values);
	}


}
