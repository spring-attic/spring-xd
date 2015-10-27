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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.xd.dirt.security.SecurityTestUtils.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.CollectionUtils;
import org.springframework.xd.dirt.security.support.UserCredentials;

/**
 * Tests for security configuration backed by a file-based user list.
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 */
@RunWith(Parameterized.class)
public class SingleNodeApplicationWithUsersFileTest {

	private static UserCredentials viewOnlyUser = new UserCredentials("bob", "bobspassword");

	private static UserCredentials adminOnlyUser = new UserCredentials("alice", "alicepwd");

	private static UserCredentials createOnlyUser = new UserCredentials("cartman", "cartmanpwd");

	private static UserCredentials wrongUsername = new UserCredentials("joe", "joespassword");

	private static UserCredentials wrongPassword = new UserCredentials("bob", "bobpassword999");

	private static UserCredentials bootUsernameWithRandomPassword = new UserCredentials("admin", "whosThere");

	private final static SpringXdResource springXdResource = new SpringXdResource(
			"classpath:org/springframework/xd/dirt/security/fileBasedUsers.yml");

	private final static Logger logger = LoggerFactory.getLogger(SingleNodeApplicationWithUsersFileTest.class);

	@ClassRule
	public static TestRule springXdAndLdapServer = springXdResource;

	@Parameters(name = "Authentication Test {index} - {0} {2} - Returns: {1}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {

			{ HttpMethod.GET, HttpStatus.OK, "/", adminOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/", null, null },

			{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/management/metrics", null, null },
			{ HttpMethod.GET, HttpStatus.OK, "/management/metrics", adminOnlyUser, null },

			{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/modules", null, null }, //Unauthenticated
			{ HttpMethod.GET, HttpStatus.OK, "/modules", viewOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/modules", wrongUsername, null },
			{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/modules", bootUsernameWithRandomPassword, null },
			{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/modules", wrongPassword, null },

			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions", createOnlyUser, null }, //AuthenticatedButUnauthorized
			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions.json", createOnlyUser, null },

			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions", adminOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/streams/definitions.json", adminOnlyUser, null },

			{ HttpMethod.GET, HttpStatus.OK, "/streams/definitions", viewOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.OK, "/streams/definitions.json", viewOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.OK, "/streams/definitions.json", viewOnlyUser,
				ImmutableMap.of("page", "0", "size", "10") },

			{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/definitions", viewOnlyUser, null },
			{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/definitions.json", viewOnlyUser, null },

			{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/definitions", adminOnlyUser, null },
			{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/definitions.json", adminOnlyUser, null },

			{ HttpMethod.DELETE, HttpStatus.OK, "/streams/definitions", createOnlyUser, null },
			{ HttpMethod.DELETE, HttpStatus.OK, "/streams/definitions.json", createOnlyUser, null },

			{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/streams/deployments/abcd", viewOnlyUser, null },
			{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/streams/deployments/abcd", adminOnlyUser, null },
			{ HttpMethod.POST, HttpStatus.NOT_FOUND, "/streams/deployments/abcd", createOnlyUser, null },

			{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/deployments/abcd", viewOnlyUser, null },
			{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/deployments/abcd", adminOnlyUser, null },
			{ HttpMethod.DELETE, HttpStatus.NOT_FOUND, "/streams/deployments/abcd", createOnlyUser, null },

			{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/definitions/abcd", viewOnlyUser, null },
			{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/streams/definitions/abcd", adminOnlyUser, null },
			{ HttpMethod.DELETE, HttpStatus.NOT_FOUND, "/streams/definitions/abcd", createOnlyUser, null },

			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/definitions", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/definitions.json", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.OK, "/jobs/definitions", viewOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.OK, "/jobs/definitions.json", viewOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/definitions.json", createOnlyUser,
				ImmutableMap.of("page", "0", "size", "10") },
			{ HttpMethod.GET, HttpStatus.OK, "/jobs/definitions.json", viewOnlyUser,
				ImmutableMap.of("page", "0", "size", "10") },

			{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/jobs/definitions", viewOnlyUser,
				ImmutableMap.of("definition", "timestampfile", "deploy", "true", "name", "abcdef") },
			{ HttpMethod.POST, HttpStatus.CREATED, "/jobs/definitions", createOnlyUser,
				ImmutableMap.of("definition", "timestampfile", "deploy", "true", "name", "abcdef") },

			{ HttpMethod.POST, HttpStatus.FORBIDDEN, "/jobs/executions", viewOnlyUser,
				ImmutableMap.of("jobname", "abcdef") },
			{ HttpMethod.PUT, HttpStatus.FORBIDDEN, "/jobs/executions", viewOnlyUser,
				ImmutableMap.of("stop", "true") },
			{ HttpMethod.PUT, HttpStatus.FORBIDDEN, "/jobs/executions", adminOnlyUser,
				ImmutableMap.of("stop", "true") },
			{ HttpMethod.PUT, HttpStatus.OK, "/jobs/executions", createOnlyUser,
				ImmutableMap.of("stop", "true") },
			{ HttpMethod.POST, HttpStatus.CREATED, "/jobs/executions", createOnlyUser,
				ImmutableMap.of("jobname", "abcdef") },

			{ HttpMethod.DELETE, HttpStatus.OK, "/jobs/definitions/abcdef", createOnlyUser, null },
			{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/jobs/definitions/abcdef", viewOnlyUser, null },

			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/configurations", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.OK, "/jobs/configurations", viewOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/configurations.json", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.OK, "/jobs/configurations.json", viewOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/configurations.json", createOnlyUser,
				ImmutableMap.of("page", "0", "size", "10") },
			{ HttpMethod.GET, HttpStatus.OK, "/jobs/configurations.json", viewOnlyUser,
				ImmutableMap.of("page", "0", "size", "10") },

			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions.json", createOnlyUser, null },

			{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/executions/333/steps/123/progress", null, null },
			{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/executions/333/steps/123/progress", viewOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/333/steps/123/progress", adminOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/333/steps/123/progress", createOnlyUser, null },

			{ HttpMethod.GET, HttpStatus.UNAUTHORIZED, "/jobs/executions/333/steps/123/progress.json", null, null },
			{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/executions/333/steps/123/progress.json", viewOnlyUser,
				null },
			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/333/steps/123/progress.json", adminOnlyUser,
				null },
			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/executions/333/steps/123/progress.json", createOnlyUser,
				null },

			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/instances", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/jobs/instances.json", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.BAD_REQUEST, "/jobs/instances", viewOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.NOT_FOUND, "/jobs/instances", viewOnlyUser,
				ImmutableMap.of("jobname", "testjobname") },

			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/modules", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/modules.json", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.OK, "/modules", viewOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.OK, "/modules.json", viewOnlyUser, null },

			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/runtime/modules", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/runtime/modules.json", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.OK, "/runtime/modules", viewOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.OK, "/runtime/modules.json", viewOnlyUser, null },

			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/runtime/containers", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/runtime/containers.json", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.OK, "/runtime/containers", viewOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.OK, "/runtime/containers.json", viewOnlyUser, null },
			{ HttpMethod.DELETE, HttpStatus.FORBIDDEN, "/runtime/containers", viewOnlyUser, null },
			{ HttpMethod.DELETE, HttpStatus.BAD_REQUEST, "/runtime/containers", createOnlyUser, null },
			{ HttpMethod.DELETE, HttpStatus.NOT_FOUND, "/runtime/containers", createOnlyUser,
				ImmutableMap.of("containerId", "123456789") },

			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/metrics/counters", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/metrics/counters.json", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.OK, "/metrics/counters", viewOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.OK, "/metrics/counters.json", viewOnlyUser, null },

			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/metrics/field-value-counters", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/metrics/field-value-counters.json", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.OK, "/metrics/field-value-counters", viewOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.OK, "/metrics/field-value-counters.json", viewOnlyUser, null },

			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/metrics/aggregate-counters", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/metrics/aggregate-counters.json", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.OK, "/metrics/aggregate-counters", viewOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.OK, "/metrics/aggregate-counters.json", viewOnlyUser, null },

			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/metrics/gauges", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/metrics/gauges.json", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.OK, "/metrics/gauges", viewOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.OK, "/metrics/gauges.json", viewOnlyUser, null },

			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/metrics/rich-gauges", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.FORBIDDEN, "/metrics/rich-gauges.json", createOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.OK, "/metrics/rich-gauges", viewOnlyUser, null },
			{ HttpMethod.GET, HttpStatus.OK, "/metrics/rich-gauges.json", viewOnlyUser, null }
		});
	}

	@Parameter(value = 0)
	public HttpMethod httpMethod;

	@Parameter(value = 1)
	public HttpStatus expectedHttpStatus;

	@Parameter(value = 2)
	public String url;

	@Parameter(value = 3)
	public UserCredentials userCredentials;

	@Parameter(value = 4)
	public Map<String, String> urlParameters;

	@Test
	public void testEndpointAuthentication() throws Exception {

		logger.info(String.format("Using parameters - httpMethod: %s, "
				+ "URL: %s, URL parameters: %s, user credentials: %s", this.httpMethod,
				this.url, this.urlParameters, userCredentials));

		final MockHttpServletRequestBuilder rb;

		switch (httpMethod) {
			case GET:
				rb = get(url);
				break;
			case POST:
				rb = post(url);
				break;
			case PUT:
				rb = put(url);
				break;
			case DELETE:
				rb = delete(url);
				break;
			default:
				throw new IllegalArgumentException("Unsupported Method: " + httpMethod);
		}

		if (this.userCredentials != null) {
			rb.header("Authorization",
					basicAuthorizationHeader(this.userCredentials.getUsername(), this.userCredentials.getPassword()));
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
			case CREATED:
				statusResultMatcher = status().isCreated();
				break;
			case BAD_REQUEST:
				statusResultMatcher = status().isBadRequest();
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
					String.format("Assertion failed for parameters - httpMethod: %s, "
							+ "URL: %s, URL parameters: %s, user credentials: %s",
							this.httpMethod, this.url, this.urlParameters, this.userCredentials),
					e);
		}
	}
}
