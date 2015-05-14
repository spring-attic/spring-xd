package org.springframework.xd.rest.client.impl;

import org.junit.Test;
import org.springframework.hateoas.UriTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

public final class JobTemplateTest {
	private final JobTemplate jobTemplate;

	private final MockRestServiceServer mockServer;

	public JobTemplateTest() {
		MockMvc mockMvc = standaloneSetup(StubController.class).build();
		ClientHttpRequestFactory clientHttpRequestFactory = new MockMvcClientHttpRequestFactory(mockMvc);
		AbstractTemplate abstractTemplate = new AbstractTemplate(clientHttpRequestFactory);
		abstractTemplate.resources.put("jobs/clean/rabbit", new UriTemplate("/jobs/clean/rabbit"));

		this.jobTemplate = new JobTemplate(abstractTemplate, "test-admin-uri", "test-password", "test-username",
				"test-vhost");
		this.mockServer = MockRestServiceServer.createServer(this.jobTemplate.restTemplate);
	}

	@Test
	public void deleteQueue() {
		this.mockServer
				.expect(requestTo("/jobs/clean/rabbit/test-queue-name?adminUri=test-admin-uri&pw=test-password&user=test-username&vhost=test-vhost"))
				.andExpect(method(HttpMethod.DELETE))
				.andRespond(withStatus(HttpStatus.OK));

		this.jobTemplate.cleanBusResources("test-queue-name");

		this.mockServer.verify();
	}

	@Controller
	private static final class StubController {
	}

}
