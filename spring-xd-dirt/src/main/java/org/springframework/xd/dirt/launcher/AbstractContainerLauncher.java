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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.dirt.event.ContainerStartedEvent;
import org.springframework.xd.dirt.event.ContainerStoppedEvent;
import org.springframework.xd.dirt.server.options.ContainerOptions;

/**
 * @author Mark Fisher
 */
public abstract class AbstractContainerLauncher implements ContainerLauncher, ApplicationEventPublisherAware,
		ApplicationContextAware {

	private volatile ApplicationEventPublisher eventPublisher;

	private volatile ApplicationContext deployerContext;

	private final Log logger = LogFactory.getLog(this.getClass());

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) {
		this.deployerContext = context;
	}

	@Override
	public XDContainer launch(ContainerOptions options) {
		try {
			String id = this.generateId();
			XDContainer container = new XDContainer(id);
			container.setApplicationContext(deployerContext);
			container.start();
			this.logContainerInfo(logger, container);
			container.addListener(new ShutdownListener(container));
			this.eventPublisher.publishEvent(new ContainerStartedEvent(container));
			return container;
		}
		catch (Exception e) {
			logger.fatal(e.getClass().getName() + " : " + e.getMessage());
			this.logErrorInfo(e);
			System.exit(1);
		}
		return null;
	}

	protected abstract String generateId();

	protected abstract void logContainerInfo(Log logger, XDContainer container);

	protected abstract void logErrorInfo(Exception exception);

	private static class ShutdownListener implements ApplicationListener<ContextClosedEvent> {

		private final XDContainer container;

		ShutdownListener(XDContainer container) {
			this.container = container;
		}

		@Override
		public void onApplicationEvent(ContextClosedEvent event) {
			event.getApplicationContext().publishEvent(new ContainerStoppedEvent(container));
			container.stop();
		}
	}

}
