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

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.ImmutableMap;

/**
 * Tests for security configuration backed by a file-based user list.
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 */
@RunWith(Parameterized.class)
public class SingleNodeApplicationWithUsersFileTest {

	private final static SpringXdResource springXdResource = new SpringXdResource(
			"classpath:org/springframework/xd/dirt/security/fileBasedUsers.yml");

	private final static Logger logger = LoggerFactory.getLogger(SingleNodeApplicationWithUsersFileTest.class);

	@ClassRule
	public static TestRule springXdAndLdapServer = springXdResource;

	@Parameters(name = "Authentication Test {index} - {1} - Returns: {0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {

			{ HttpStatus.OK, "/", "alice", "alicepwd", null },
			{ HttpStatus.UNAUTHORIZED, "/", null, null, null },

			{ HttpStatus.UNAUTHORIZED, "/management/metrics", null, null, null },
			{ HttpStatus.OK, "/management/metrics", "alice", "alicepwd", null },

			{ HttpStatus.UNAUTHORIZED, "/modules", null, null, null }, //Unauthenticated
			{ HttpStatus.OK, "/modules", "bob", "bobspassword", null },
			{ HttpStatus.UNAUTHORIZED, "/modules", "joe", "joespassword", null }, //Wrong username
			{ HttpStatus.UNAUTHORIZED, "/modules", "admin", "whosThere", null }, //Boot username fails
			{ HttpStatus.UNAUTHORIZED, "/modules", "bob", "bobpassword999", null }, //Wrong Password

			{ HttpStatus.FORBIDDEN, "/streams/definitions", "cartman", "cartmanpwd", null }, //AuthenticatedButUnauthorized
			{ HttpStatus.FORBIDDEN, "/streams/definitions.xml", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/streams/definitions.json", "cartman", "cartmanpwd", null },

			{ HttpStatus.FORBIDDEN, "/jobs/definitions", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/jobs/definitions.xml", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/jobs/definitions.json", "cartman", "cartmanpwd", null },
			{ HttpStatus.OK, "/jobs/definitions", "bob", "bobspassword", null },
			{ HttpStatus.OK, "/jobs/definitions.xml", "bob", "bobspassword", null },
			{ HttpStatus.OK, "/jobs/definitions.json", "bob", "bobspassword", null },
			{ HttpStatus.FORBIDDEN, "/jobs/definitions.json", "cartman", "cartmanpwd",
				ImmutableMap.of("page", "0", "size", "10") },
			{ HttpStatus.OK, "/jobs/definitions.json", "bob", "bobspassword",
				ImmutableMap.of("page", "0", "size", "10") },

			{ HttpStatus.FORBIDDEN, "/jobs/configurations", "cartman", "cartmanpwd", null },
			{ HttpStatus.OK, "/jobs/configurations", "bob", "bobspassword", null },
			{ HttpStatus.FORBIDDEN, "/jobs/configurations.xml", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/jobs/configurations.json", "cartman", "cartmanpwd", null },
			{ HttpStatus.OK, "/jobs/configurations.json", "bob", "bobspassword", null },
			{ HttpStatus.FORBIDDEN, "/jobs/configurations.json", "cartman", "cartmanpwd",
				ImmutableMap.of("page", "0", "size", "10") },
			{ HttpStatus.OK, "/jobs/configurations.json", "bob", "bobspassword",
				ImmutableMap.of("page", "0", "size", "10") },

			{ HttpStatus.FORBIDDEN, "/jobs/executions", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/jobs/executions.xml", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/jobs/executions.json", "cartman", "cartmanpwd", null },

			{ HttpStatus.FORBIDDEN, "/jobs/instances", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/jobs/instances.xml", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/jobs/instances.json", "cartman", "cartmanpwd", null },
			{ HttpStatus.INTERNAL_SERVER_ERROR, "/jobs/instances", "bob", "bobspassword", null },
			{ HttpStatus.NOT_FOUND, "/jobs/instances", "bob", "bobspassword",
				ImmutableMap.of("jobname", "testjobname") },

			{ HttpStatus.FORBIDDEN, "/modules", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/modules.xml", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/modules.json", "cartman", "cartmanpwd", null },
			{ HttpStatus.OK, "/modules", "bob", "bobspassword", null },
			{ HttpStatus.OK, "/modules.xml", "bob", "bobspassword", null },
			{ HttpStatus.OK, "/modules.json", "bob", "bobspassword", null },

			{ HttpStatus.FORBIDDEN, "/runtime/modules", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/runtime/modules.xml", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/runtime/modules.json", "cartman", "cartmanpwd", null },
			{ HttpStatus.OK, "/runtime/modules", "bob", "bobspassword", null },
			{ HttpStatus.OK, "/runtime/modules.xml", "bob", "bobspassword", null },
			{ HttpStatus.OK, "/runtime/modules.json", "bob", "bobspassword", null },

			{ HttpStatus.FORBIDDEN, "/runtime/containers", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/runtime/containers.xml", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/runtime/containers.json", "cartman", "cartmanpwd", null },
			{ HttpStatus.OK, "/runtime/containers", "bob", "bobspassword", null },
			{ HttpStatus.OK, "/runtime/containers.xml", "bob", "bobspassword", null },
			{ HttpStatus.OK, "/runtime/containers.json", "bob", "bobspassword", null },

			{ HttpStatus.FORBIDDEN, "/metrics/counters", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/metrics/counters.xml", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/metrics/counters.json", "cartman", "cartmanpwd", null },
			{ HttpStatus.OK, "/metrics/counters", "bob", "bobspassword", null },
			{ HttpStatus.OK, "/metrics/counters.xml", "bob", "bobspassword", null },
			{ HttpStatus.OK, "/metrics/counters.json", "bob", "bobspassword", null },

			{ HttpStatus.FORBIDDEN, "/metrics/field-value-counters", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/metrics/field-value-counters.xml", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/metrics/field-value-counters.json", "cartman", "cartmanpwd", null },
			{ HttpStatus.OK, "/metrics/field-value-counters", "bob", "bobspassword", null },
			{ HttpStatus.OK, "/metrics/field-value-counters.xml", "bob", "bobspassword", null },
			{ HttpStatus.OK, "/metrics/field-value-counters.json", "bob", "bobspassword", null },

			{ HttpStatus.FORBIDDEN, "/metrics/aggregate-counters", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/metrics/aggregate-counters.xml", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/metrics/aggregate-counters.json", "cartman", "cartmanpwd", null },
			{ HttpStatus.OK, "/metrics/aggregate-counters", "bob", "bobspassword", null },
			{ HttpStatus.OK, "/metrics/aggregate-counters.xml", "bob", "bobspassword", null },
			{ HttpStatus.OK, "/metrics/aggregate-counters.json", "bob", "bobspassword", null },

			{ HttpStatus.FORBIDDEN, "/metrics/gauges", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/metrics/gauges.xml", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/metrics/gauges.json", "cartman", "cartmanpwd", null },
			{ HttpStatus.OK, "/metrics/gauges", "bob", "bobspassword", null },
			{ HttpStatus.OK, "/metrics/gauges.xml", "bob", "bobspassword", null },
			{ HttpStatus.OK, "/metrics/gauges.json", "bob", "bobspassword", null },

			{ HttpStatus.FORBIDDEN, "/metrics/rich-gauges", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/metrics/rich-gauges.xml", "cartman", "cartmanpwd", null },
			{ HttpStatus.FORBIDDEN, "/metrics/rich-gauges.json", "cartman", "cartmanpwd", null },
			{ HttpStatus.OK, "/metrics/rich-gauges", "bob", "bobspassword", null },
			{ HttpStatus.OK, "/metrics/rich-gauges.xml", "bob", "bobspassword", null },
			{ HttpStatus.OK, "/metrics/rich-gauges.json", "bob", "bobspassword", null }
		});
	}

	@Parameter(value = 0)
	public HttpStatus expectedHttpStatus;

	@Parameter(value = 1)
	public String url;

	@Parameter(value = 2)
	public String username;

	@Parameter(value = 3)
	public String password;

	@Parameter(value = 4)
	public Map<String, String> urlParameters;

	@Test
	public void testEndpointAuthentication() throws Exception {

		logger.info(String.format("Using parameters - "
				+ "URL: %s, URL parameters: %s, username: %s, password: %s",
				this.url, this.urlParameters, this.username, this.password));

		final MockHttpServletRequestBuilder rb = get(url);

		if (this.username != null && this.password != null) {
			rb.header("Authorization", basicAuthorizationHeader(this.username, this.password));
		}

		if (!CollectionUtils.isEmpty(urlParameters)) {
			for (Map.Entry<String, String> mapEntry : urlParameters.entrySet()) {
				rb.param(mapEntry.getKey(), mapEntry.getValue());
			}
		}

		final ResultMatcher statusResultMatcher;

		switch (expectedHttpStatus) {
			case UNAUTHORIZED:
				statusResultMatcher = status().isUnauthorized();
				break;
			case FORBIDDEN:
				statusResultMatcher = status().isForbidden();
				break;
			case NOT_FOUND:
				statusResultMatcher = status().isNotFound();
				break;
			case OK:
				statusResultMatcher = status().isOk();
				break;
			case INTERNAL_SERVER_ERROR:
				statusResultMatcher = status().isInternalServerError();
				break;
			default:
				throw new IllegalArgumentException("Unsupported Status: " + expectedHttpStatus);
		}

		try {
			springXdResource.getMockMvc().perform(rb).andDo(print()).andExpect(statusResultMatcher);
		}
		catch (AssertionError e) {
			throw new AssertionError(
					String.format("Assertion failed for parameters - "
							+ "URL: %s, URL parameters: %s, username: %s, password: %s",
							this.url, this.urlParameters, this.username, this.password), e);
		}
	}

}
