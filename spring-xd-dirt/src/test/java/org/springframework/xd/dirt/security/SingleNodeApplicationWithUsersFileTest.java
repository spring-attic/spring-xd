/*
 *
 *  * Copyright 2011-2015 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.xd.dirt.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.xd.dirt.security.SecurityTestUtils.basicAuthorizationHeader;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

/**
 * Tests for security configuration backed by a file-based user list.
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 */
public class SingleNodeApplicationWithUsersFileTest {

	private final static SpringXdResource springXdResource = new SpringXdResource(
			"classpath:org/springframework/xd/dirt/security/fileBasedUsers.yml");

	@ClassRule
	public static TestRule springXdAndLdapServer = springXdResource;

	@Test
	public void testUnauthenticatedAccessToModulesEndpointFails() throws Exception {
		springXdResource.getMockMvc()
				.perform(get("/modules"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void testUnauthenticatedAccessToManagementEndpointFails() throws Exception {
		springXdResource.getMockMvc()
				.perform(get("/management/metrics"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void testAuthenticatedAccessToModulesEndpointSucceeds() throws Exception {
		springXdResource.getMockMvc()
				.perform(get("/modules").header("Authorization", basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	public void testWrongUsernameFails() throws Exception {
		springXdResource.getMockMvc()
				.perform(get("/modules").header("Authorization", basicAuthorizationHeader("joe", "joespassword")))
				.andDo(print())
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void testDefaultSpringBootConfigurationFails() throws Exception {
		springXdResource.getMockMvc()
				.perform(get("/modules").header("Authorization", basicAuthorizationHeader("admin", "whosThere")))
				.andDo(print())
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void testWrongPasswordFails() throws Exception {
		springXdResource.getMockMvc()
				.perform(get("/modules").header("Authorization", basicAuthorizationHeader("bob", "bobpassword999")))
				.andDo(print())
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void testAuthenticatedAccessToManagementEndpointSucceeds() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/management/metrics").header("Authorization",
								basicAuthorizationHeader("alice", "alicepwd")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	// Stream Definitions

	@Test
	public void testAuthenticatedButUnauthorizedAccessToDefinitionsEndpoint() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/streams/definitions").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToDefinitionsEndpointWithXmlExtension() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/streams/definitions.xml").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToDefinitionsEndpointWithJsonExtension() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/streams/definitions.json").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	// Job Definitions

	@Test
	public void testAuthenticatedButUnauthorizedAccessToJobDefinitionsEndpoint() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/jobs/definitions").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToJobDefinitionsEndpointWithXmlExtension() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/jobs/definitions.xml").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToJobDefinitionsEndpointWithJsonExtension() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/jobs/definitions.json").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	// Job Deployments

	// Job Configurations

	@Test
	public void testAuthenticatedButUnauthorizedAccessToJobConfigurationsEndpoint() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/jobs/configurations").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToJobConfigurationsEndpointWithXmlExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/jobs/configurations.xml").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToJobConfigurationsEndpointWithJsonExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/jobs/configurations.json").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	// Job Executions

	@Test
	public void testAuthenticatedButUnauthorizedAccessToJobExecutionsEndpoint() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/jobs/executions").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToJobExecutionsEndpointWithXmlExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/jobs/executions.xml").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToJobExecutionsEndpointWithJsonExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/jobs/executions.json").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	// Job Instances

	@Test
	public void testAuthenticatedButUnauthorizedAccessToJobInstancesEndpoint() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/jobs/instances").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToJobInstancesEndpointWithXmlExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/jobs/instances.xml").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToJobInstancesEndpointWithJsonExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/jobs/instances.json").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToJobInstancesEndpoint() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/jobs/instances").param("jobname", "testjobname").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isNotFound());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToJobInstancesEndpointWithJsonExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/jobs/instances.json").param("jobname", "testjobname").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isNotFound());
	}

	// Modules

	@Test
	public void testAuthenticatedButUnauthorizedAccessToModulesEndpoint() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/modules").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToModulesEndpointWithXmlExtension() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/modules.xml").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToModulesEndpointWithJsonExtension() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/modules.json").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToModulesEndpoint() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/modules").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToModulesEndpointWithXmlExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/modules.xml").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToModulesEndpointWithJsonExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/modules.json").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	// Runtime Modules

	@Test
	public void testAuthenticatedButUnauthorizedAccessToRuntimeModulesEndpoint() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/runtime/modules").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToRuntimeModulesEndpointWithXmlExtension() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/runtime/modules.xml").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToRuntimeModulesEndpointWithJsonExtension() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/runtime/modules.json").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToRuntimeModulesEndpoint() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/runtime/modules").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToRuntimeModulesEndpointWithXmlExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/runtime/modules.xml").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToRuntimeModulesEndpointWithJsonExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/runtime/modules.json").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	// Runtime Containers

	@Test
	public void testAuthenticatedButUnauthorizedAccessToRuntimeContainersEndpoint() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/runtime/containers").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToRuntimeContainersEndpointWithXmlExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/runtime/containers.xml").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToRuntimeContainersEndpointWithJsonExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/runtime/containers.json").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToRuntimeContainersEndpoint() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/runtime/containers").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToRuntimeContainersEndpointWithXmlExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/runtime/containers.xml").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToRuntimeContainersEndpointWithJsonExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/runtime/containers.json").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	// Counters

	@Test
	public void testAuthenticatedButUnauthorizedAccessToCountersEndpoint() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/counters").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToCountersEndpointWithXmlExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/counters.xml").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToCountersEndpointWithJsonExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/counters.json").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToCountersEndpoint() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/counters").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToCountersEndpointWithXmlExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/counters.xml").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToCountersEndpointWithJsonExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/counters.json").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	// Field Value Counters

	@Test
	public void testAuthenticatedButUnauthorizedAccessToFieldValueCountersEndpoint() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/field-value-counters").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToFieldValueCountersEndpointWithXmlExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/field-value-counters.xml").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToFieldValueCountersEndpointWithJsonExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/field-value-counters.json").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToFieldValueCountersEndpoint() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/field-value-counters").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToFieldValueCountersEndpointWithXmlExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/field-value-counters.xml").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToFieldValueCountersEndpointWithJsonExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/field-value-counters.json").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	// Aggregate Counters

	@Test
	public void testAuthenticatedButUnauthorizedAccessToAggregateCountersEndpoint() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/aggregate-counters").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToAggregateCountersEndpointWithXmlExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/aggregate-counters.xml").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToAggregateCountersEndpointWithJsonExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/aggregate-counters.json").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToAggregateCountersEndpoint() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/aggregate-counters").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToAggregateCountersEndpointWithXmlExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/aggregate-counters.xml").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToAggregateCountersEndpointWithJsonExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/aggregate-counters.json").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	// Gauges

	@Test
	public void testAuthenticatedButUnauthorizedAccessToGaugesEndpoint() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/gauges").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToGaugesEndpointWithXmlExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/gauges.xml").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToGaugesEndpointWithJsonExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/gauges.json").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToGaugesEndpoint() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/gauges").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToGaugesEndpointWithXmlExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/gauges.xml").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToGaugesEndpointWithJsonExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/gauges.json").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	// Rich Gauges

	@Test
	public void testAuthenticatedButUnauthorizedAccessToRichGaugesEndpoint() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/rich-gauges").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToRichGaugesEndpointWithXmlExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/rich-gauges.xml").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedButUnauthorizedAccessToRichGaugesEndpointWithJsonExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/rich-gauges.json").header("Authorization",
								basicAuthorizationHeader("cartman", "cartmanpwd")))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToRichGaugesEndpoint() throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/rich-gauges").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToRichGaugesEndpointWithXmlExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/rich-gauges.xml").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	public void testAuthenticatedAndAuthorizedAccessToRichGaugesEndpointWithJsonExtension()
			throws Exception {
		springXdResource.getMockMvc()
				.perform(
						get("/metrics/rich-gauges.json").header("Authorization",
								basicAuthorizationHeader("bob", "bobspassword")))
				.andDo(print())
				.andExpect(status().isOk());
	}

	// Tab Completions


}
