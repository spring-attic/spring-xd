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

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.container.DefaultContainer;
import org.springframework.xd.dirt.core.Container;
import org.springframework.xd.dirt.event.ContainerStartedEvent;

/**
 * @author Mark Fisher
 * @author Jennifer Hickey
 * @author David Turanski
 */
public class DefaultContainerLauncher implements ContainerLauncher, ApplicationEventPublisherAware {

	private volatile ApplicationEventPublisher eventPublisher;

	private static Log logger = LogFactory.getLog(DefaultContainerLauncher.class);

	public DefaultContainerLauncher() {
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public Container launch() {
		DefaultContainer container = new DefaultContainer();
		container.start();
		container.addListener(new ShutdownListener(container));
		this.eventPublisher.publishEvent(new ContainerStartedEvent(container));
		return container;
	}

	public static void main(String[] args) {
		String xdhome = System.getProperty("xd.home");
		if (!StringUtils.hasText(xdhome)) {
			xdhome = (args.length > 0) ? args[0] : "..";
			System.setProperty("xd.home", xdhome);
		}
		String transportType = System.getProperty("xd.transport");
		if (!StringUtils.hasText(transportType)) {
			transportType = (args.length > 1) ? args[1] : "local";
			System.setProperty("xd.transport",transportType);
		}
		
		logger.info("xd.home=" + new File(xdhome).getAbsolutePath());
		logger.info("xd.transport=" + transportType);

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("META-INF/spring/launcher.xml");

		context.registerShutdownHook();
		ContainerLauncher launcher = context.getBean(ContainerLauncher.class);
		launcher.launch();
	}

	private static class ShutdownListener implements ApplicationListener<ContextClosedEvent> {

		private final Container container;

		ShutdownListener(Container container) {
			this.container = container;
		}

		@Override
		public void onApplicationEvent(ContextClosedEvent event) {
			container.stop();
		}
	}

}
