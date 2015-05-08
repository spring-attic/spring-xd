/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.xd.rest.client.impl;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import org.junit.Test;

import org.springframework.hateoas.UriTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

/**
*
* @author Paul Harris
* @author Gary Russell
* @since 1.2
*/
public final class StreamTemplateTests {

	private final StreamTemplate streamTemplate;

	private final MockRestServiceServer mockServer;

	public StreamTemplateTests() {
		MockMvc mockMvc = standaloneSetup(StubController.class).build();
		ClientHttpRequestFactory clientHttpRequestFactory = new MockMvcClientHttpRequestFactory(mockMvc);
		AbstractTemplate abstractTemplate = new AbstractTemplate(clientHttpRequestFactory);
		abstractTemplate.resources.put("streams/clean/rabbit", new UriTemplate("/streams/clean/rabbit"));

		this.streamTemplate = new StreamTemplate(abstractTemplate, "test-admin-uri", "test-password", "test-username",
				"test-vhost", "test-prefix.");
		this.mockServer = MockRestServiceServer.createServer(this.streamTemplate.restTemplate);
	}

	@Test
	public void deleteQueue() {
		this.mockServer
				.expect(requestTo("/streams/clean/rabbit/test-queue-name?adminUri=test-admin-uri"
						+ "&pw=test-password&user=test-username&vhost=test-vhost&busPrefix=test-prefix."))
				.andExpect(method(HttpMethod.DELETE))
				.andRespond(withStatus(HttpStatus.OK));

		this.streamTemplate.cleanBusResources("test-queue-name");

		this.mockServer.verify();
	}

	@Controller
	private static final class StubController {
	}

}
