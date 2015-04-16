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

package org.springframework.xd.dirt.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.xd.dirt.security.SecurityTestUtils.basicAuthorizationHeader;

import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author Marius Bogoevici
 */
public class SingleNodeApplicationWithUserBasedSecurityTest {

	@ClassRule
	public static SpringXdResource springXdResource = new SpringXdResource(
			"classpath:org/springframework/xd/dirt/security/simpleSecurity.yml");

	@Test
	public void testUnauthenticatedAccessToModulesEndpointFails() throws Exception {
		springXdResource.getMockMvc()
				.perform(get("/modules"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", "Basic realm=\"SpringXD\"")
				);
	}

	@Test
	public void testUnauthenticatedAccessToModulesEndpointWithJsonExtensionFails() throws Exception {
		springXdResource.getMockMvc()
				.perform(get("/modules.json"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", "Basic realm=\"SpringXD\"")
				);
	}

	@Test
	public void testUnauthenticatedAccessToManagementEndpointFails() throws Exception {
		springXdResource.getMockMvc()
				.perform(get("/management/metrics"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", "Basic realm=\"SpringXD\"")
				);
	}

	@Test
	public void testAuthenticatedAccessToModulesEndpointSucceeds() throws Exception {
		springXdResource.getMockMvc()
				.perform(get("/modules").header("Authorization", basicAuthorizationHeader("admin", "whosThere")))
				.andDo(print())
				.andExpect(status().isOk()
				);
	}

	@Test
	public void testAuthenticatedAccessToManagementEndpointSucceeds() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/management/metrics").header("Authorization",
								basicAuthorizationHeader("admin", "whosThere")))
				.andDo(print())
				.andExpect(status().isOk());
	}
}
