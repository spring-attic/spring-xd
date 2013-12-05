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

import org.springframework.context.ApplicationListener;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;


/**
 * Initializer that can print useful stuff about an XD context on startup.
 * 
 * @author Dave Syer
 * @author David Turanski
 */
public class XdConfigLoggingInitializer implements ApplicationListener<ContextRefreshedEvent>, EnvironmentAware {

	private static Log logger = LogFactory.getLog(XdConfigLoggingInitializer.class);

	protected Environment environment;

	private final boolean isContainer;

	public XdConfigLoggingInitializer(boolean isContainer) {
		this.isContainer = isContainer;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		logger.info("XD home: " + environment.resolvePlaceholders("${XD_HOME}"));
		if (isContainer) {
			logger.info("Data Transport: " + environment.resolvePlaceholders("${XD_TRANSPORT}"));
		}
		logger.info("Control Transport: " + environment.resolvePlaceholders("${XD_CONTROL_TRANSPORT}"));
		logger.info("Store: " + environment.resolvePlaceholders("${XD_STORE}"));
		logger.info("Analytics: " + environment.resolvePlaceholders("${XD_ANALYTICS}"));
	}

}
