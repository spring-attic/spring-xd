/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.launcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.Environment;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.dirt.server.options.ContainerOptions;
import org.springframework.xd.dirt.server.options.XDPropertyKeys;
import org.springframework.xd.dirt.server.util.BannerUtils;

/**
 * @author Mark Fisher
 */
public abstract class AbstractContainerLauncher implements ContainerLauncher, ApplicationContextAware {

	private volatile ApplicationContext launcherContext;

	private final Log logger = LogFactory.getLog(this.getClass());

	@Override
	public void setApplicationContext(ApplicationContext context) {
		this.launcherContext = context;
	}

	@Override
	public XDContainer launch(ContainerOptions options) {
		try {
			String id = this.generateId();
			XDContainer container = new XDContainer(id);
			container.setLauncherContext(launcherContext);
			container.start();
			this.logContainerInfo(logger, container);
			container.addListener(new ShutdownListener(container));
			return container;
		}
		catch (Exception e) {
			logger.fatal(e.getClass().getName() + " : " + e.getMessage());
			this.logErrorInfo(e);
			throw new XDContainerLaunchException(e.getMessage(), e);
		}
	}

	protected abstract String generateId();

	protected abstract String getRuntimeInfo(XDContainer container);

	protected abstract void logErrorInfo(Exception exception);

	private static class ShutdownListener implements ApplicationListener<ContextClosedEvent> {

		private final XDContainer container;

		ShutdownListener(XDContainer container) {
			this.container = container;
		}

		@Override
		public void onApplicationEvent(ContextClosedEvent event) {
			container.stop();
		}
	}

	protected void logContainerInfo(Log logger, XDContainer container) {
		if (logger.isInfoEnabled()) {
			StringBuilder runtimeInfo = new StringBuilder();
			runtimeInfo.append(this.getRuntimeInfo(container));
			if (container.isJmxEnabled()) {
				runtimeInfo.append(String.format("\nMBean Server: http://localhost:%d/jolokia/", container.getJmxPort()));
			}
			else {
				runtimeInfo.append(" JMX is disabled for XD components");
			}
			runtimeInfo.append(logXDEnvironment(container));
			logger.info(BannerUtils.displayBanner(container.getJvmName(), runtimeInfo.toString()));
		}
	}

	private String logXDEnvironment(XDContainer container) {
		Environment environment = container.getApplicationContext().getEnvironment();
		String[] keys = new String[] { XDPropertyKeys.XD_HOME, XDPropertyKeys.XD_TRANSPORT,
			XDPropertyKeys.XD_STORE, XDPropertyKeys.XD_ANALYTICS, XDPropertyKeys.XD_HADOOP_DISTRO };
		StringBuilder sb = new StringBuilder("\nXD Configuration:\n");
		for (String key : keys) {
			sb.append("\t" + key + "=" + environment.getProperty(key) + "\n");
		}
		return sb.toString();
	}
}
