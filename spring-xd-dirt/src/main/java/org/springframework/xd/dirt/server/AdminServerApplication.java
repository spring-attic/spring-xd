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

import javax.servlet.Filter;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchDatabaseInitializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.event.SourceFilteringListener;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.web.filter.HttpPutFormContentFilter;
import org.springframework.xd.batch.XdBatchDatabaseInitializer;
import org.springframework.xd.dirt.rest.RestConfiguration;
import org.springframework.xd.dirt.server.options.AdminOptions;
import org.springframework.xd.dirt.server.options.CommandLinePropertySourceOverridingListener;
import org.springframework.xd.dirt.util.BannerUtils;
import org.springframework.xd.dirt.util.ConfigLocations;
import org.springframework.xd.dirt.util.XdConfigLoggingInitializer;
import org.springframework.xd.dirt.util.XdProfiles;

@Configuration
@EnableAutoConfiguration(exclude = { BatchAutoConfiguration.class, JmxAutoConfiguration.class })
@ImportResource("classpath:" + ConfigLocations.XD_INTERNAL_CONFIG_ROOT + "admin-server.xml")
@Import(RestConfiguration.class)
public class AdminServerApplication {

	private static final String MBEAN_EXPORTER_BEAN_NAME = "XDAdminMBeanExporter";

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

		this.context = new SpringApplicationBuilder(AdminOptions.class, ParentConfiguration.class)
				.profiles(XdProfiles.ADMIN_PROFILE)
				.listeners(commandLineListener)
				.child(SharedServerContextConfiguration.class, AdminOptions.class)
				.listeners(commandLineListener)
				.child(AdminServerApplication.class)
				.listeners(commandLineListener)
				.initializers(new AdminIdInitializer())
				.run(args);
		return this;
	}

	private class AdminIdInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			String adminContextId = applicationContext.getId();
			applicationContext.setId(XdProfiles.ADMIN_PROFILE
					+ adminContextId.substring(adminContextId.lastIndexOf(":")));
		}
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

	@ConditionalOnExpression("${XD_JMX_ENABLED:true}")
	@EnableMBeanExport(defaultDomain = "xd.admin")
	protected static class JmxConfiguration {

		@Bean(name = MBEAN_EXPORTER_BEAN_NAME)
		public IntegrationMBeanExporter integrationMBeanExporter() {
			IntegrationMBeanExporter exporter = new IntegrationMBeanExporter();
			exporter.setDefaultDomain("xd.admin");
			return exporter;
		}
	}

	@Bean
	public BatchDatabaseInitializer batchDatabaseInitializer() {
		return new XdBatchDatabaseInitializer();
	}
}
