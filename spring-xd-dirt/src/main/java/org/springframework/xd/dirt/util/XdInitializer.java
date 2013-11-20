/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationInitializer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;


/**
 * Initializer that can print useful stuff about an XD context on startup.
 * 
 * @author Dave Syer
 */
public class XdInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>,
		SpringApplicationInitializer, EnvironmentAware, Ordered {

	private static Log logger = LogFactory.getLog(XdInitializer.class);

	private Environment environment;

	private int order = Ordered.LOWEST_PRECEDENCE;

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void initialize(SpringApplication springApplication, String[] args) {
		springApplication.setShowBanner(false);
	}

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		logger.info("XD home: " + environment.resolvePlaceholders("${XD_HOME}"));
		logger.info("Transport: " + environment.resolvePlaceholders("${XD_TRANSPORT}"));
		logger.info("Store: " + environment.resolvePlaceholders("${XD_STORE}"));
		logger.info("Analytics: " + environment.resolvePlaceholders("${XD_ANALYTICS}"));
	}

}
