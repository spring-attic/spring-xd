/*
 * Copyright 2013 the original author or authors.
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
 */

package org.springframework.xd.module.options;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.validation.BindException;


public class PojoModuleOptionsMetadataTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private PojoModuleOptionsMetadata metadata = new PojoModuleOptionsMetadata(BackingPojo.class);

	@Test
	public void testDefaultValues() {
		assertThat(metadata, hasItem(both(hasName("foo")).and(hasDefaultValue("somedefault"))));
		assertThat(metadata, hasItem(both(hasName("bar")).and(hasDefaultValue("42"))));
	}

	@Test
	public void testPropertyListing() {
		assertThat(metadata, hasItem(hasName("foo")));
		assertThat(metadata, hasItem(hasName("bar")));
		assertThat(metadata, not(hasItem(hasName("fooBar"))));
	}

	@Test
	public void testSuccessfulInterpolation() throws BindException {
		Map<String, String> values = new HashMap<String, String>();
		values.put("foo", "othervalue");
		values.put("bar", "1729");

		ModuleOptions options = metadata.interpolate(values);
		EnumerablePropertySource<?> propertySource = options.asPropertySource();
		assertThat(Arrays.asList(propertySource.getPropertyNames()), containsInAnyOrder("foo", "fooBar"));
		assertThat((String) propertySource.getProperty("foo"), equalTo("othervalue"));
		assertThat((String) propertySource.getProperty("fooBar"), equalTo("othervalue1729"));
	}

	@Test
	public void testSuccessfulInterpolationDefaults() throws BindException {
		Map<String, String> values = new HashMap<String, String>();

		ModuleOptions options = metadata.interpolate(values);
		EnumerablePropertySource<?> propertySource = options.asPropertySource();
		assertThat(Arrays.asList(propertySource.getPropertyNames()), containsInAnyOrder("foo", "fooBar"));
		assertThat((String) propertySource.getProperty("foo"), equalTo("somedefault"));
		assertThat((String) propertySource.getProperty("fooBar"), equalTo("somedefault42"));
	}

	@Test
	public void testUnknownOption() throws BindException {
		Map<String, String> values = new HashMap<String, String>();
		values.put("foo", "othervalue");
		values.put("wizz", "1729");

		thrown.expect(BindException.class);
		thrown.expectMessage("wizz");

		metadata.interpolate(values);

	}

	@Test
	public void testBindingWrongType() throws BindException {
		Map<String, String> values = new HashMap<String, String>();
		values.put("foo", "othervalue");
		values.put("bar", "17.29");

		thrown.expect(BindException.class);
		thrown.expectMessage("bar");
		thrown.expectMessage("int");

		metadata.interpolate(values);

	}

	@Test
	public void testBindingJsr303() throws BindException {
		Map<String, String> values = new HashMap<String, String>();
		values.put("foo", "othervalue");
		values.put("bar", "65536");

		thrown.expect(BindException.class);
		thrown.expectMessage("bar");
		thrown.expectMessage("int");
		thrown.expectMessage("65536");
		thrown.expectMessage("10000");

		metadata.interpolate(values);

	}

	@Test
	public void testProfilesActivation() throws BindException {
		Map<String, String> values = new HashMap<String, String>();
		values.put("bar", "1729");

		ModuleOptions options = metadata.interpolate(values);
		assertThat(options.profilesToActivate(), arrayContaining("high-profile"));

		metadata = new PojoModuleOptionsMetadata(BackingPojo.class);
		values.clear();
		options = metadata.interpolate(values);
		assertThat(options.profilesToActivate().length, equalTo(0));

	}

	@Test
	public void testGroupValidation() throws BindException {
		Map<String, String> values = new HashMap<String, String>();
		values.put("foo", "othervalue");
		values.put("bar", "2065");

		thrown.expect(BindException.class);
		thrown.expectMessage("foo");
		thrown.expectMessage("length must be < 3 when bar > 2000");

		metadata.interpolate(values);

	}

	private Matcher<ModuleOption> hasName(String name) {
		return hasProperty("name", equalTo(name));
	}

	private Matcher<ModuleOption> hasDefaultValue(Object value) {
		return hasProperty("defaultValue", equalTo(value));
	}


}
