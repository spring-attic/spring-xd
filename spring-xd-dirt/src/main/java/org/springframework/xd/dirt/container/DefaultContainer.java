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

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;
import org.springframework.context.ApplicationListener;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.Container;
import org.springframework.xd.dirt.event.ContainerStartedEvent;
import org.springframework.xd.dirt.event.ContainerStoppedEvent;

/**
 * @author Mark Fisher
 * @author Jennifer Hickey
 * @author David Turanski
 */
public class DefaultContainer implements Container, SmartLifecycle {

	private static final String LINE_SEPARATOR = System
			.getProperty("line.separator");

	private static final Log logger = LogFactory.getLog(DefaultContainer.class);

	/**
	 * Base location for XD config files. Chosen so as not to collide with user provided content.
	 */
	public static final String XD_CONFIG_ROOT = "META-INF/spring-xd/";

	/**
	 * Where container related config files reside.
	 */
	public static final String XD_INTERNAL_CONFIG_ROOT = XD_CONFIG_ROOT + "internal/";

	private static final String CORE_CONFIG = XD_INTERNAL_CONFIG_ROOT + "container.xml";

	// TODO: consider moving to a file: location pattern within $XD_HOME
	private static final String PLUGIN_CONFIGS = "classpath*:" + XD_CONFIG_ROOT + "plugins/*.xml";

	private static final String LOG4J_FILE_APPENDER = "file";

	private volatile AbstractApplicationContext context;

	private final String id;

	private volatile String jvmName;

	/**
	 * Creates a container with a given id
	 * @param id the id
	 */
	public DefaultContainer(String id) {
		this.id = id;
	}

	/**
	 * Default constructor generates a random id
	 */
	public DefaultContainer() {
		this.id = UUID.randomUUID().toString();
	}

	@Override
	public String getId() {
		return (this.context != null) ? this.context.getId() : "";
	}

	@Override
	public String getJvmName() {
		synchronized (this) {
			if (this.jvmName == null) {
				this.jvmName = ManagementFactory.getRuntimeMXBean().getName();
			}
		}
		return this.jvmName;
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
		updateLoggerFilename();
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
			this.context.publishEvent(new ContainerStoppedEvent(this));
			this.context.close();
			if (logger.isInfoEnabled()) {
				final String message = "Stopped container: " + this.jvmName;
				final StringBuilder sb = new StringBuilder(LINE_SEPARATOR);
				sb.append(StringUtils.rightPad("", message.length(), "-"))
						.append(LINE_SEPARATOR)
						.append(message)
						.append(LINE_SEPARATOR)
						.append(StringUtils.rightPad("", message.length(), "-"))
						.append(LINE_SEPARATOR);
				logger.info(sb.toString());
			}
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

	/**
	 * Update container log appender file name with container id
	 */
	private void updateLoggerFilename() {
		Appender appender = Logger.getRootLogger().getAppender(LOG4J_FILE_APPENDER);
		if (appender instanceof RollingFileAppender) {
			// the xd.home system property is always set at this point
			((RollingFileAppender) appender).setFile(
					new File(System.getProperty("xd.home")).getAbsolutePath() + "/logs/container-" + this.getId() + ".log");
			((RollingFileAppender) appender).activateOptions();
		}
	}

}
