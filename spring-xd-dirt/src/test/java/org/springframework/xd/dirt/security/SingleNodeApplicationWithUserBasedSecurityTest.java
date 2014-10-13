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

import org.junit.Test;

/**
 * @author Marius Bogoevici
 */
@WithSpringConfigLocation("classpath:org/springframework/xd/dirt/security/simpleSecurity.yml")
public class SingleNodeApplicationWithUserBasedSecurityTest extends AbstractSingleNodeApplicationSecurityTest {

	@Test
	public void testUnauthenticatedAccessToModulesEndpointFails() throws Exception {
		mockMvc().perform(
				get("/modules"))
				.andExpect(
						status().isUnauthorized()
				).andExpect(header().string("WWW-Authenticate", "Basic realm=\"SpringXD\"")
		);
	}

	@Test
	public void testUnauthenticatedAccessToManagementEndpointFails() throws Exception {
		mockMvc().perform(
				get("/management/metrics"))
				.andExpect(
						status().isUnauthorized()
				).andExpect(header().string("WWW-Authenticate", "Basic realm=\"SpringXD\"")
		);
	}

	@Test
	public void testAuthenticatedAccessToModulesEndpointSucceeds() throws Exception {
		mockMvc()
				.perform(
						get("/modules")
								.header("Authorization", basicAuthorizationHeader("admin", "whosThere"))
				).andDo(print()).andExpect(
				status().isOk()
		);
	}

	@Test
	public void testAuthenticatedAccessToManagementEndpointSucceeds() throws Exception {
		mockMvc()
				.perform(
						get("/management/metrics")
								.header("Authorization", basicAuthorizationHeader("admin", "whosThere"))
				).andDo(print())
				.andExpect(
						status().isOk()
				);
	}
}
