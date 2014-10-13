/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.server;

import java.io.IOException;
import java.net.ServerSocket;

import javax.servlet.Filter;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.autoconfigure.AuditAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchDatabaseInitializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.event.SourceFilteringListener;
import org.springframework.web.filter.HttpPutFormContentFilter;
import org.springframework.xd.batch.XdBatchDatabaseInitializer;
import org.springframework.xd.dirt.cluster.RuntimeAttributes;
import org.springframework.xd.dirt.rest.RestConfiguration;
import org.springframework.xd.dirt.server.options.AdminOptions;
import org.springframework.xd.dirt.server.options.CommandLinePropertySourceOverridingListener;
import org.springframework.xd.dirt.util.BannerUtils;
import org.springframework.xd.dirt.util.ConfigLocations;
import org.springframework.xd.dirt.util.RuntimeUtils;
import org.springframework.xd.dirt.util.XdConfigLoggingInitializer;
import org.springframework.xd.dirt.util.XdProfiles;
import org.springframework.xd.dirt.web.WebConfiguration;

@Configuration
@EnableAutoConfiguration(exclude = { BatchAutoConfiguration.class, JmxAutoConfiguration.class,
	AuditAutoConfiguration.class })
@ImportResource("classpath:" + ConfigLocations.XD_INTERNAL_CONFIG_ROOT + "admin-server.xml")
@Import({ RestConfiguration.class, WebConfiguration.class })
public class AdminServerApplication {

	private static final Log logger = LogFactory.getLog(AdminServerApplication.class);

	private ConfigurableApplicationContext context;

	public static void main(String[] args) {
		new AdminServerApplication().run(args);
	}

	public ConfigurableApplicationContext getContext() {
		return this.context;
	}

	public AdminServerApplication run(String... args) {
		System.out.println(BannerUtils.displayBanner(getClass().getSimpleName(), null));

		CommandLinePropertySourceOverridingListener<AdminOptions> commandLineListener = new CommandLinePropertySourceOverridingListener<AdminOptions>(
				new AdminOptions());

		try {
			this.context = new SpringApplicationBuilder(AdminOptions.class, ParentConfiguration.class)
					.logStartupInfo(false)
					.profiles(XdProfiles.ADMIN_PROFILE)
					.listeners(commandLineListener)
					.initializers(new AdminPortAvailablityInitializer())
					.child(SharedServerContextConfiguration.class, AdminOptions.class)
					.logStartupInfo(false)
					.listeners(commandLineListener)
					.child(AdminServerApplication.class)
					.listeners(commandLineListener)
					.initializers(new AdminIdInitializer())
					.run(args);
		}
		catch (Exception e) {
			handleException(e);
		}
		return this;
	}

	private void handleException(Exception e) {
		String errorMessage;
		// There could be few other reasons for the failure when starting the embedded servletContainer
		if (e.getCause() instanceof EmbeddedServletContainerException) {
			errorMessage = String.format(
					"Error starting embedded servlet container (tomcat) for the admin server: %s",
					ExceptionUtils.getRootCause(e));
		}
		else {
			errorMessage = e.getMessage();
		}
		logger.error(errorMessage);
		System.exit(1);
	}

	private class AdminIdInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			String adminContextId = applicationContext.getId();
			applicationContext.setId(RuntimeUtils.getIpAddress()
					+ adminContextId.substring(adminContextId.lastIndexOf(":")));
		}
	}

	/**
	 * {@link ApplicationContextInitializer} that checks if the admin port is already in use.
	 *
	 * @author Ilayaperumal Gopinathan
	 */
	private class AdminPortAvailablityInitializer implements
			ApplicationContextInitializer<ConfigurableApplicationContext> {

		private static final String ADMIN_PORT_PLACEHOLDER = "${server.port}";

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			String adminPort = applicationContext.getEnvironment().resolvePlaceholders(ADMIN_PORT_PLACEHOLDER);
			ServerSocket socket = null;
			try {
				socket = new ServerSocket(Integer.parseInt(adminPort));
			}
			catch (IOException e) {
				throw new AdminPortNotAvailableException(adminPort);
			}
			finally {
				if (socket != null) {
					try {
						socket.close();
					}
					catch (IOException e) {
						// It is very unlikely to get an exception here.
					}
				}
			}
		}
	}

	@Bean
	public RuntimeAttributes runtimeAttributes() {
		RuntimeAttributes runtimeAttributes = new RuntimeAttributes();
		runtimeAttributes.setHost(RuntimeUtils.getHost()).setIp(RuntimeUtils.getIpAddress()).setPid(
				RuntimeUtils.getPid());
		return runtimeAttributes;
	}

	@Bean
	@ConditionalOnWebApplication
	public Filter httpPutFormContentFilter() {
		return new HttpPutFormContentFilter();
	}

	@Bean
	public ApplicationListener<?> xdInitializer(ApplicationContext context) {
		XdConfigLoggingInitializer delegate = new XdConfigLoggingInitializer(false);
		delegate.setEnvironment(context.getEnvironment());
		return new SourceFilteringListener(context, delegate);
	}

	@Bean
	public BatchDatabaseInitializer batchDatabaseInitializer() {
		return new XdBatchDatabaseInitializer();
	}
}
