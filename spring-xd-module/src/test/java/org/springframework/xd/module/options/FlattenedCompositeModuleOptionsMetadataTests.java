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

import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
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


public class FlattenedCompositeModuleOptionsMetadataTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();


	private FlattenedCompositeModuleOptionsMetadata flattened = new FlattenedCompositeModuleOptionsMetadata(
			Arrays.asList(
					new PojoModuleOptionsMetadata(BackingPojo.class),
					new PojoModuleOptionsMetadata(OtherBackingPojo.class)));

	@Test
	@SuppressWarnings("unchecked")
	public void testNamesIteration() {
		assertThat(flattened,
				containsInAnyOrder(moduleOptionNamed("bar"), moduleOptionNamed("fizz"), moduleOptionNamed("foo")));
	}

	@Test
	public void testProfileActivation() throws BindException {
		Map<String, String> values = new HashMap<String, String>();
		values.put("bar", "1000");
		values.put("fizz", "bonjour");

		assertThat(flattened.interpolate(values).profilesToActivate(),
				arrayContainingInAnyOrder("french-profile", "high-profile"));

	}

	@Test
	public void testValidation() throws BindException {
		Map<String, String> values = new HashMap<String, String>();
		values.put("foo", "othervalue");
		values.put("bar", "65536");

		thrown.expect(BindException.class);
		thrown.expectMessage("bar");
		thrown.expectMessage("int");
		thrown.expectMessage("65536");
		thrown.expectMessage("10000");

		flattened.interpolate(values);
	}

	@Test
	public void testSuccessfulInterpolation() throws Exception {
		Map<String, String> values = new HashMap<String, String>();
		values.put("foo", "value_of_foo");
		values.put("bar", "59");
		values.put("fizz", "value_of_fizz");
		ModuleOptions interpolated = flattened.interpolate(values);

		EnumerablePropertySource<?> ps = interpolated.asPropertySource();
		assertThat(ps.getPropertyNames(), arrayContainingInAnyOrder("fizz", "foo", "fooBar"));
		assertThat(ps.getProperty("fizz"), equalTo((Object) "value_of_fizz"));
		assertThat(ps.getProperty("foo"), equalTo((Object) "value_of_foo"));
		assertThat(ps.getProperty("fooBar"), equalTo((Object) "value_of_foo59"));

	}

	@Test
	public void testUnknownOption() throws BindException {
		Map<String, String> values = new HashMap<String, String>();
		values.put("foo", "othervalue");
		values.put("wizz", "1729");

		thrown.expect(BindException.class);
		thrown.expectMessage("wizz");

		flattened.interpolate(values);
	}

	@Test
	public void testBindingWrongType() throws BindException {
		Map<String, String> values = new HashMap<String, String>();
		values.put("foo", "othervalue");
		values.put("bar", "17.29");

		thrown.expect(BindException.class);
		thrown.expectMessage("bar");
		thrown.expectMessage("int");

		flattened.interpolate(values);

	}

	@Test
	public void testDefaultValues() throws BindException {
		Map<String, String> values = new HashMap<String, String>();
		ModuleOptions interpolated = flattened.interpolate(values);

		EnumerablePropertySource<?> ps = interpolated.asPropertySource();
		assertThat(ps.getPropertyNames(), arrayContainingInAnyOrder("fizz", "foo", "fooBar"));
		assertThat(ps.getProperty("fizz"), equalTo((Object) "hello"));
		assertThat(ps.getProperty("foo"), equalTo((Object) "somedefault"));
		assertThat(ps.getProperty("fooBar"), equalTo((Object) "somedefault42"));
	}


	private Matcher<ModuleOption> moduleOptionNamed(String name) {
		return hasProperty("name", equalTo(name));
	}
}
