/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.xd.extensions;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertySource;
import org.springframework.xd.dirt.container.decryptor.PropertiesDecryptor;
import org.springframework.xd.dirt.server.TestApplicationBootstrap;
import org.springframework.xd.dirt.server.singlenode.SingleNodeApplication;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;

/**
 * @author David Turanski
 * @since 1.3.1
 **/
public class PropertiesDecryptorTests {
	private static SingleNodeApplication application;

	private static ConfigurableApplicationContext context;

	private static String originalConfigLocation;

	@BeforeClass
	public static void setUp() {
		originalConfigLocation = System.getProperty("spring.config.location");
		System.setProperty("spring.config.location",
				"classpath:extensions/properties-decryptor-test.yml");
		application = new TestApplicationBootstrap().getSingleNodeApplication().run();
	}

	@Test
	public void test(){
		assertThat(application.containerContext().getEnvironment().getProperty("foo.bar"),
				is("abcdef"));
		assertThat(application.adminContext().getEnvironment().getProperty("foo.bar"),
				is("abcdef"));
		assertThat(application.pluginContext().getEnvironment().getProperty("foo.bar"),
				is("abcdef"));

		assertTrue(application.containerContext().getParent().getParent()
				.containsBean
				(PropertiesDecryptor.DECRYPTOR_BEAN_NAME));
	}

	@AfterClass
	public static void close() {
		if (application != null) {
			application.close();
		}
		if (originalConfigLocation != null) {
			System.setProperty("spring.config.location", originalConfigLocation);
		}
		else {
			System.clearProperty("spring.config.location");
		}
	}
}
