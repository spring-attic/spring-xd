/*
 * Copyright 2011-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.module;

import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 *
 * 
 * @author Eric Bottard
 */
public class CustomModuleRegistryFactoryBean implements FactoryBean<WritableModuleRegistry>, InitializingBean{

	private static final Logger logger = LoggerFactory.getLogger(CustomModuleRegistryFactoryBean.class);

	private static final Pattern NO_SYNCHRONIZATION_PATTERN = Pattern.compile("^file:.*");

	private WritableModuleRegistry registry;

	private final String root;

	public CustomModuleRegistryFactoryBean(String root) {
		this.root = root;
	}

	@Override
	public WritableModuleRegistry getObject() throws Exception {
		return registry;
	}

	@Override
	public Class<WritableModuleRegistry> getObjectType() {
		return WritableModuleRegistry.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Matcher matcher = NO_SYNCHRONIZATION_PATTERN.matcher(root);
		if (matcher.matches()) {
			registry = new ResourceModuleRegistry(root, true);
			((ResourceModuleRegistry)registry).afterPropertiesSet();
			logger.info("Custom modules will be written directly to {}", root);
		} else {
			String localRoot = "file:" + Files.createTempDirectory("spring-xd-custom-modules");
			ResourceModuleRegistry local = new ResourceModuleRegistry(localRoot, true);
			local.afterPropertiesSet();

			ResourceModuleRegistry remote = new ResourceModuleRegistry(root, true);
			remote.afterPropertiesSet();

			registry = new SynchronizingModuleRegistry(remote, local);
			logger.info("Custom modules will be written at {} and kept in synch locally at {}", root, localRoot);
		}
	}
}
