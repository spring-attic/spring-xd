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

import javax.servlet.Filter;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.xd.dirt.server.SingleNodeApplication;

/**
 * Base class for Security Tests - allows for starting single node applications with different
 * configuration options - for testing different security scenarios.
 *
 * Supports the {@link org.springframework.xd.dirt.security.WithSpringConfigLocation} annotation that
 * allows a simple way to indicate what configuration file should the test be started with.
 *
 * @author Marius Bogoevici
 */
public abstract class AbstractSingleNodeApplicationSecurityTest {

	private static MockMvc mockMvc;

	private static SingleNodeApplication singleNodeApplication;

	private static String originalConfigLocation = null;

	private static String adminPort;

	protected RestTemplate restTemplate;

	@ClassRule
	public static ExternalResource server = new ExternalResource() {

		@Override
		public Statement apply(Statement base, Description description) {
			WithSpringConfigLocation springConfigLocationAnnotation = AnnotationUtils.findAnnotation(description.getTestClass(), WithSpringConfigLocation.class);
			originalConfigLocation = System.getProperty("spring.config.location");
			if (springConfigLocationAnnotation == null || StringUtils.isEmpty(springConfigLocationAnnotation.value())) {
			} else {
				System.setProperty("spring.config.location", springConfigLocationAnnotation.value());
			}
			return super.apply(base, description);
		}

		@Override
		protected void before() throws Throwable {
			singleNodeApplication = new SingleNodeApplication();
			SingleNodeApplication run = singleNodeApplication.run();
			ConfigurableApplicationContext configurableApplicationContext = run.adminContext();
			mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) configurableApplicationContext)
					                 .addFilters(configurableApplicationContext.getBeansOfType(Filter.class).values().toArray(new Filter[]{}))
					                 .build();
			adminPort = application().adminContext().getEnvironment().resolvePlaceholders("${server.port}");
		}

		@Override
		protected void after() {
			singleNodeApplication.close();
			if (originalConfigLocation != null) {
				System.setProperty("spring.config.location", originalConfigLocation);
			} else {
				System.clearProperty("spring.config.location");
			}
		}
	};

	static String basicAuthorizationHeader(String username, String password) {
		return "Basic " + new String(Base64.encode((username + ":" + password).getBytes()));
	}

	public static MockMvc mockMvc() {
		return mockMvc;
	}

	public static SingleNodeApplication application() {
		return singleNodeApplication;
	}

	protected static String adminPort() {
		return adminPort;
	}

	@Before
	public void setUpRestTemplate() throws Exception {
		SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
		// set this value as low as possible, since we're connecting locally
		simpleClientHttpRequestFactory.setConnectTimeout(500);
		simpleClientHttpRequestFactory.setReadTimeout(2000);
		restTemplate = new RestTemplate(simpleClientHttpRequestFactory);
	}
}
