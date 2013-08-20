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

package org.springframework.xd.dirt.launcher;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;

import org.springframework.xd.dirt.core.Container;
import org.springframework.xd.dirt.server.options.ContainerOptions;

import org.springframework.xd.dirt.server.util.BannerUtils;

/**
 * 
 * @author David Turanski
 */
public class LocalContainerLauncher extends AbstractContainerLauncher {
	private static AtomicLong id = new AtomicLong();

	@Override
	protected String generateId() {
		return String.valueOf(id.getAndIncrement());
	}

	@Override
	protected void logContainerInfo(Log logger, Container container, ContainerOptions options) {
		if (logger.isInfoEnabled()) {
			final StringBuilder runtimeInfo = new StringBuilder();
			runtimeInfo.append("Using local mode");
			if (!options.isJmxEnabled()) {
				runtimeInfo.append(" JMX is disabled for XD components");
			}
			else {
				runtimeInfo.append(String.format(" JMX port: %d", options.getJmxPort()));
			}
			logger.info(BannerUtils.displayBanner(container.getJvmName(), runtimeInfo.toString()));
		}
	}

	@Override
	protected void logErrorInfo(Exception exception) {
	}

}
