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

package org.springframework.xd.dirt.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author Gunnar Hillert
 */
@SpringApplicationConfiguration(classes = { WebConfiguration.class })
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class SecurityDisabledConfigurationTests {

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private Environment environment;

	protected MockMvc mockMvc;

	@BeforeClass
	public static void beforeClass() {
		System.clearProperty("security.basic.enabled");
	}

	@Before
	public void setupMockMVC() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
	}

	@Test
	public void testThatSecurityIsNotEnabled() throws Exception {
		Assert.assertNull("'security.basic.enabled' property should be null",
				environment.getProperty("security.basic.enabled", Boolean.class));
	}

	@Test
	public void testBasicAuthentication() throws Exception {
		mockMvc.perform(get("/").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isNotFound());
	}

	@Test
	public void testFormAuthentication() throws Exception {
		mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
		.andExpect(status().isNotFound());
	}

	@Test
	public void testThatBasicAuthenticationIsTriggeredByMediaTypeAll() throws Exception {
		mockMvc.perform(get("/").accept(MediaType.ALL))
		.andExpect(status().isNotFound());
	}
}
