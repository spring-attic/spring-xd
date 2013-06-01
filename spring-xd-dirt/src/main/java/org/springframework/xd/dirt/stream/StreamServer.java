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

package org.springframework.xd.dirt.stream;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.integration.MessagingException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.server.PipeProtocol;

/**
 * This is a temporary "server" for the REST API. Currently it only handles simple
 * stream configurations (tokens separated by pipes) without any parameters. This
 * will be completely replaced by a more robust solution. Intended for demo only.
 *
 * @author Mark Fisher
 * @author Jennifer Hickey
 */
public class StreamServer implements SmartLifecycle, InitializingBean {

	private final Log logger = LogFactory.getLog(getClass());

	private volatile String contextPath = "";

	private volatile String servletName = "streams";

	private volatile int port = 8080;

	private volatile Tomcat tomcat = new Tomcat();

	private volatile ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

	private volatile ScheduledFuture<?> handlerTask = null;

	private volatile boolean running;

	private final StreamDeployer streamDeployer;

	public StreamServer(StreamDeployer streamDeployer) {
		Assert.notNull(streamDeployer, "streamDeployer must not be null");
		this.streamDeployer = streamDeployer;
	}

	/**
	 * Set the port. Default is: 8080
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Set the context path. Default is empty.
	 */
	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	/**
	 * Set the servlet name. Default is: streams
	 */
	public void setServletName(String servletName) {
		this.servletName = servletName;
	}

	@Override
	public void afterPropertiesSet() {
		this.scheduler.setPoolSize(3);
		this.scheduler.initialize();
		this.tomcat.setPort(this.port);
		String path = (this.contextPath.startsWith("/")) ? this.contextPath : "/" + this.contextPath;
		Context context = this.tomcat.addContext(path, new File(".").getAbsolutePath());
		Tomcat.addServlet(context, this.servletName, new XdServlet());
		context.addServletMapping("/" + this.servletName + "/*", this.servletName);
		if (logger.isInfoEnabled()) {
			logger.info("initialized server: context=" + this.contextPath + ", servlet=" + this.servletName);
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

	@SuppressWarnings("serial")
	private class XdServlet extends HttpServlet {
		@Override
		protected void service(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {
			String streamName = request.getPathInfo();
			Assert.hasText(streamName, "no stream name (e.g. localhost/streams/streamname");
			streamName = streamName.replaceAll("/", "");
			if ("POST".equalsIgnoreCase(request.getMethod())) {
				String streamConfig = FileCopyUtils.copyToString(request.getReader());
				streamDeployer.deployStream(streamName, streamConfig);
			}
			else if ("DELETE".equalsIgnoreCase(request.getMethod())) {
				streamDeployer.undeployStream(streamName);
			}
			else {
				response.sendError(405);
			}
		}
	}

	public static void main(String[] args) {
		setXDHome(args);
		setActiveProfile(args);
		try {
			new ClassPathXmlApplicationContext("META-INF/spring/admin.xml");
		}
		catch(RedisConnectionFailureException e) {
			final Log logger = LogFactory.getLog(StreamServer.class);
			logger.fatal(e.getMessage());
			System.err.println("Redis does not seem to be running. Did you install and start Redis? " +
					"Please see the Getting Started section of the guide for instructions.");
			System.exit(1);
		}
	}
	
	/**
	 * Set xd.home system property
	 * @param args
	 */
	private static void setXDHome(String[] args) {
		String xdhome = System.getProperty("xd.home");
		if (!StringUtils.hasText(xdhome)) {
			xdhome = (args.length > 0) ? args[0] : "..";
			System.setProperty("xd.home", xdhome);
		}
	}
	
	/**
	 * Set spring.profiles.active system property
	 * @param args
	 */
	private static void setActiveProfile(String[] args){
		if (args.length > 1 && StringUtils.hasText(args[1])){
			// Set the pipe protocol as active profile
			System.setProperty("spring.profiles.active", args[1]);
		} else {
			// Set the redis profile as default profile 
			System.setProperty("spring.profiles.active", PipeProtocol.REDIS.toString());
		}
	}
}
