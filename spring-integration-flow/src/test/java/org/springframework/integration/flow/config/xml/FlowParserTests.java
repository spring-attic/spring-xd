/*
 * Copyright 2002-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.flow.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.flow.Flow;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author David Turanski
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class FlowParserTests {
	@Autowired
	Flow simple;

	@Test
	public void testImplicitModuleName() {
		assertEquals("simple",simple.getName());
	}

	@Autowired
	Flow basic2;

	@Test
	public void testExplicitProperties() {
		assertNotNull(basic2.getProperties());
		assertEquals("bar", basic2.getProperties().get("foo"));
		assertEquals("simple", basic2.getName());
		assertEquals("basic2", basic2.getInstanceId());
		assertEquals("flow", basic2.getType());
	}

	@Autowired
	Flow basic3;

	@Test
	public void testInlineProperties() {
		assertNotNull(basic3.getProperties());
		assertEquals("bar", basic3.getProperties().get("foo"));
		assertEquals("car", basic3.getProperties().get("baz"));
		assertEquals("simple", basic3.getName());
		assertEquals("basic3", basic3.getInstanceId());
		assertEquals("flow", basic3.getType());
	}

	@Autowired
	Flow basic4;

	@Test
	public void testActiveProfiles() {
		assertNotNull(basic4.getApplicationContext().getEnvironment().getActiveProfiles());
		assertEquals(2, basic4.getApplicationContext().getEnvironment().getActiveProfiles().length);
		List<String> profiles = Arrays.asList(basic4.getApplicationContext().getEnvironment().getActiveProfiles());
		assertTrue(profiles.contains("foo"));
		assertTrue(profiles.contains("bar"));
	}

	@Autowired
	Flow basic5;

	@Test
	public void testAdditionalComponentLocations() {
		assertNotNull(basic5.getAdditionalComponentLocations());
		assertEquals(2, basic5.getAdditionalComponentLocations().size());
	}

	@Autowired
	Flow basic6;

	@Test
	public void testPropertiesRef() {
		assertNotNull(basic6.getProperties());
		assertEquals("bar", basic6.getProperties().get("foo"));
	}
}
