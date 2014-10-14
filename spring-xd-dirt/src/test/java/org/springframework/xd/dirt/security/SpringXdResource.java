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

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.xd.dirt.server.SingleNodeApplication;

/**
* @author Marius Bogoevici
*/
public class SpringXdResource extends ExternalResource {

	private String originalConfigLocation = null;

	private SingleNodeApplication singleNodeApplication;

	private MockMvc mockMvc;

	private String adminPort;

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
		singleNodeApplication.run();
		ConfigurableApplicationContext configurableApplicationContext = singleNodeApplication.adminContext();
		mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) configurableApplicationContext)
								 .addFilters(configurableApplicationContext.getBeansOfType(Filter.class).values().toArray(new Filter[]{}))
								 .build();
		adminPort = singleNodeApplication.adminContext().getEnvironment().resolvePlaceholders("${server.port}");
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

	public MockMvc getMockMvc() {
		return mockMvc;
	}

	public String getAdminPort() {
		return adminPort;
	}
}
