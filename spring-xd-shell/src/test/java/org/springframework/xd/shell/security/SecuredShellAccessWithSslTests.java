/*
 * Copyright 2014-2015 the original author or authors.
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


package org.springframework.xd.shell.security;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

import java.net.URI;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.hateoas.PagedResources;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.xd.dirt.server.singlenode.SingleNodeApplication;
import org.springframework.xd.rest.client.impl.SpringXDTemplate;
import org.springframework.xd.rest.domain.ModuleDefinitionResource;
import org.springframework.xd.rest.domain.RESTModuleType;
import org.springframework.xd.test.RandomConfigurationSupport;

/**
 * Tests a mixed security/SSL scenario, directly with the SpringXDTemplate. This allows a more relaxed
 * handling of SSL certificates, such as supporting self-signed certificates in the test.
 *
 * @author Marius Bogoevici
 */
public class SecuredShellAccessWithSslTests {

	private static SingleNodeApplication singleNodeApplication;

	private static String originalConfigLocation;

	private static String adminPort;

	@BeforeClass
	public static void setUp() throws Exception {
		RandomConfigurationSupport randomConfigurationSupport = new RandomConfigurationSupport();
		originalConfigLocation = System.getProperty("spring.config.location");
		System.setProperty("spring.config.location",
				"classpath:org/springframework/xd/shell/security/securedServerWithSsl.yml");
		singleNodeApplication = new SingleNodeApplication().run();
		adminPort = randomConfigurationSupport.getAdminServerPort();
	}

	@Test
	public void testSpringXDTemplate() throws Exception {
		BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("admin", "whosThere"));
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
				HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider).setSSLSocketFactory(
						new SSLConnectionSocketFactory(
								new SSLContextBuilder().loadTrustMaterial(null,
										new TrustSelfSignedStrategy()).build())).build());
		SpringXDTemplate template = new SpringXDTemplate(requestFactory, new URI("https://localhost:" + adminPort));
		PagedResources<ModuleDefinitionResource> moduleDefinitions = template.moduleOperations().list(
				RESTModuleType.sink);
		assertThat(moduleDefinitions.getLinks().size(), greaterThan(0));
	}

	@AfterClass
	public static void tearDown() throws Exception {
		singleNodeApplication.close();
		if (originalConfigLocation == null) {
			System.clearProperty("spring.config.location");
		}
		else {
			System.setProperty("spring.config.location", originalConfigLocation);
		}
	}
}
