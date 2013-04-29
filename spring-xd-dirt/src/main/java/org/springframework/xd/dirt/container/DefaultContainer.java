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

package org.springframework.xd.dirt.container;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationListener;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.xd.dirt.core.Container;
import org.springframework.xd.dirt.event.ContainerStartedEvent;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public class DefaultContainer implements Container, SmartLifecycle {

	private static final Log logger = LogFactory.getLog(DefaultContainer.class);

	private static final String CORE_CONFIG = "classpath:META-INF/spring/container.xml";

	// TODO: consider moving to a file: location pattern within $XD_HOME
	private static final String PLUGIN_CONFIGS = "classpath*:META-INF/spring/plugins/*.xml";

	private volatile AbstractApplicationContext context;

	private final String id;

	public DefaultContainer(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return (this.context != null) ? this.context.getId() : "";
	}

	@Override
	public int getPhase() {
		return 0;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public boolean isRunning() {
		return this.context != null;
	}

	@Override
	public void start() {
		this.context = new ClassPathXmlApplicationContext(new String[]{CORE_CONFIG, PLUGIN_CONFIGS}, false);
		context.setId(this.id);
		context.registerShutdownHook();
		context.refresh();
		if (logger.isInfoEnabled()) {
			logger.info("started container: " + context.getId());
		}
		context.publishEvent(new ContainerStartedEvent(this));		
	}

	@Override
	public void stop() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Override
	public void stop(Runnable callback) {
		this.stop();
		callback.run();
	}

	public void addListener(ApplicationListener<?> listener) {
		Assert.state(this.context != null, "context is not initialized");
		this.context.addApplicationListener(listener);
	}

}
