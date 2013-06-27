/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.xd.dirt.stream;

import java.io.File;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.MessagingException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * @author Mark Fisher
 * @author Jennifer Hickey
 * @author Gary Russell
 * @author David Turanski
 */
public class StreamServer implements SmartLifecycle, InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private volatile String contextPath = "";

	private volatile String servletName = "xd";

	private final int port;

	private volatile Tomcat tomcat = new Tomcat();

	private volatile ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

	private volatile ScheduledFuture<?> handlerTask = null;

	private volatile boolean running;

	private final ConfigurableWebApplicationContext webApplicationContext;

	public StreamServer(
			ConfigurableWebApplicationContext webApplicationContext, int port) {
		Assert.notNull(webApplicationContext, "context must not be null");
		Assert.isTrue(!webApplicationContext.isActive(),
				"context must not have been started");
		this.webApplicationContext = webApplicationContext;
		this.port = port;
	}

	/**
	 * Set the contextPath to serve requests on. Empty string for root.
	 */
	public void setContextPath(String contextPath) {
		if (StringUtils.hasLength(contextPath) && !contextPath.startsWith("/")) {
			contextPath = "/" + contextPath;
		}
		this.contextPath = contextPath;
	}

	/**
	 * Set the servletName. Default is 'xd'.
	 * @param servletName
	 */
	public void setServletName(String servletName) {
		this.servletName = servletName;
	}

	/**
	 * @return the HTTP port
	 */
	public int getPort() {
		return this.port;
	}

	@Override
	public void afterPropertiesSet() {
		this.scheduler.setPoolSize(3);
		this.scheduler.initialize();
		this.tomcat.setPort(this.port);
		Context tomcatContext = this.tomcat.addContext(this.contextPath,
				new File(".").getAbsolutePath());
		this.webApplicationContext.setServletContext(tomcatContext
				.getServletContext());
		this.webApplicationContext.refresh();
		Tomcat.addServlet(tomcatContext, this.servletName,
				new DispatcherServlet(this.webApplicationContext));
		tomcatContext.addServletMapping("/", this.servletName);
		if (logger.isInfoEnabled()) {
			logger.info("initialized server: context=" + this.contextPath
					+ ", servlet=" + this.servletName);
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public int getPhase() {
		return 0;
	}

	@Override
	public void start() {
		this.tomcat.getServer().addLifecycleListener(new AprLifecycleListener());
		try {
			this.tomcat.start();
			this.handlerTask = this.scheduler.schedule(new Handler(), new Date());
			if (logger.isInfoEnabled()) {
				logger.info("started embedded tomcat adapter");
			}
			this.running = true;
		}
		catch (LifecycleException e) {
			throw new MessagingException("failed to start server", e);
		}
	}

	@Override
	public void stop() {
		try {
			if (this.handlerTask != null) {
				this.handlerTask.cancel(true);
			}
			this.tomcat.stop();
			this.running = false;
		}
		catch (LifecycleException e) {
			throw new MessagingException("failed to stop server", e);
		}
	}

	@Override
	public void stop(Runnable callback) {
		this.stop();
		callback.run();
	}

	private class Handler implements Runnable {
		@Override
		public void run() {
			tomcat.getServer().await();
		}
	}

}
