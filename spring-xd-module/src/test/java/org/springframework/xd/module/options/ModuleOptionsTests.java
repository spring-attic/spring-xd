/*
 * Copyright 2013 the original author or authors.
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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Properties;
import java.util.Set;

import org.junit.Test;

import org.springframework.core.env.PropertySource;


/**
 * Test for {@link ModuleOptions} and related classes.
 * 
 * @author Eric Bottard
 */
public class ModuleOptionsTests {

	private ModuleOptions moduleOptions = new ModuleOptions();

	@Test
	public void testProfileActivation() {
		moduleOptions.addProfileActivationRule("use-cron", "cron != null");
		moduleOptions.addProfileActivationRule("use-delay", "delay != null");

		Properties props = new Properties();
		props.setProperty("cron", "0 * * * *");

		Set<String> profiles = moduleOptions.interpolate(props).getActivatedProfiles();

		assertThat(profiles, contains("use-cron"));
		assertThat(profiles, not(contains("use-delay")));
	}

	@Test
	public void testPropertySource() {
		moduleOptions.add(ModuleOption.named("cron"));
		moduleOptions.add(ModuleOption.named("delay").withDefaultValue("10").withType(int.class));
		moduleOptions.add(ModuleOption.named("longer-delay").withDefaultExpression("delay + 10").withType(int.class));

		Properties props = new Properties();
		props.setProperty("delay", "5");
		PropertySource<?> ps = moduleOptions.interpolate(props).asPropertySource();

		assertThat(ps.getProperty("cron"), is(nullValue()));
		assertThat((Integer) ps.getProperty("delay"), equalTo(5));
		assertThat((Integer) ps.getProperty("longer-delay"), equalTo(15));

	}

	@Test
	public void testTypeSupport() {
		moduleOptions.add(ModuleOption.named("greet"));
		moduleOptions.add(ModuleOption.named("ihaveaquote"));
		moduleOptions.add(ModuleOption.named("greek"));
		moduleOptions.add(ModuleOption.named("grok").withType(Integer.class));

		Properties props = new Properties();
		props.setProperty("greet", "hello");
		props.setProperty("greek", "12");
		props.setProperty("ihaveaquote", "My name's \"Q\"");
		props.setProperty("grok", "42");
		PropertySource<?> ps = moduleOptions.interpolate(props).asPropertySource();

		assertEquals("12", ps.getProperty("greek"));
		assertEquals(42, ps.getProperty("grok"));
		assertEquals("hello", ps.getProperty("greet"));
		assertEquals("My name's \"Q\"", ps.getProperty("ihaveaquote"));

	}
}
