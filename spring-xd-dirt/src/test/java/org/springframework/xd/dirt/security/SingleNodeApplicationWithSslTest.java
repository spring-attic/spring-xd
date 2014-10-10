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
import static org.junit.Assert.assertThat;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * @author Marius Bogoevici
 */

@WithSpringConfigLocation("classpath:org/springframework/xd/dirt/security/sslEnabled.yml")
public class SingleNodeApplicationWithSslTest extends AbstractSingleNodeApplicationSecurityTest {

	@ClassRule
	public static SpringXdResource springXdResource = new SpringXdResource();

	protected RestTemplate restTemplate;

	@Before
	public void setUpRestTemplate() throws Exception {
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder().loadTrustMaterial(null,
						new TrustSelfSignedStrategy()).build());

		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory)
				.build();

		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		restTemplate = new RestTemplate(requestFactory);
	}

	@Test
	public void testSslEnabled() throws Exception {
		// we will ask specifically for localhost so that the certificate matches
		ResponseEntity<Object> responseEntity = restTemplate.getForEntity("https://localhost:" + springXdResource.getAdminPort() + "/modules", Object.class);
		assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.OK));
	}

}
