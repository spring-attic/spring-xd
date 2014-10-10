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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.net.ssl.SSLException;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * @author Marius Bogoevici
 */

public class SingleNodeApplicationWithDefaultSecurityTest {

	@ClassRule
	public static SpringXdResource springXdResource = new SpringXdResource("classpath:org/springframework/xd/dirt/security/defaultSecurity.yml");

	private RestTemplate restTemplate;

	@Before
	public void setUpRestTemplate() throws Exception {
		SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
		// set this value as low as possible, since we're connecting locally
		simpleClientHttpRequestFactory.setConnectTimeout(500);
		simpleClientHttpRequestFactory.setReadTimeout(2000);
		restTemplate = new RestTemplate(simpleClientHttpRequestFactory);
	}

	@Test
	public void testModuleEndpointIsNotSecuredByDefault() throws Exception {
        springXdResource.getMockMvc()
				.perform(get("/modules"))
				.andExpect(status().isOk());
	}

	@Test
	public void testManagementEndpointIsNotSecuredByDefault() throws Exception {
		springXdResource.getMockMvc()
				.perform(get("/management/metrics"))
				.andExpect(status().isOk());
	}

	@Test
	public void testSslNotEnabledByDefaultForAdminEndpoints() throws Exception {
		try {
			restTemplate.getForEntity("https://localhost" + ":" + springXdResource.getAdminPort() + "/modules", Object.class);
		} catch (RestClientException e) {
			// the request fails because the protocol is not HTTPS
			assertThat(e.getCause(), instanceOf(SSLException.class));
		}
		// HTTP, however, succeeds
		ResponseEntity<Object> responseEntity = restTemplate.getForEntity("http://localhost" + ":" + springXdResource.getAdminPort() + "/modules", Object.class);
		assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.OK));
	}

	@Test
	public void testSslNotEnabledByDefaultForManagementEndpoints() throws Exception {
		try {
			// the request fails because the protocol is not HTTPS
			restTemplate.getForEntity("https://localhost" + ":" + springXdResource.getAdminPort() + "/management/metrics", Object.class);
		} catch (RestClientException e) {
			assertThat(e.getCause(), instanceOf(SSLException.class));
		}
		// HTTP, however, succeeds
		ResponseEntity<Object> responseEntity = restTemplate.getForEntity("http://localhost" + ":" + springXdResource.getAdminPort() + "/management/metrics", Object.class);
		assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.OK));
	}
}
