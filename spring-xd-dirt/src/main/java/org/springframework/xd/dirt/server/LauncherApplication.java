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

package org.springframework.xd.dirt.server;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.event.SourceFilteringListener;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.xd.dirt.container.ContainerStartedEvent;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.dirt.server.options.CommandLinePropertySourceOverridingInitializer;
import org.springframework.xd.dirt.server.options.ContainerOptions;
import org.springframework.xd.dirt.server.options.XDPropertyKeys;
import org.springframework.xd.dirt.util.BannerUtils;
import org.springframework.xd.dirt.util.XdConfigLoggingInitializer;

@Configuration
@EnableAutoConfiguration
@ImportResource({
	"classpath:" + XDContainer.XD_INTERNAL_CONFIG_ROOT + "launcher.xml",
	"classpath:" + XDContainer.XD_INTERNAL_CONFIG_ROOT + "container.xml",
	"classpath*:" + XDContainer.XD_CONFIG_ROOT + "plugins/*.xml" })
public class LauncherApplication {

	private static final String MBEAN_EXPORTER_BEAN_NAME = "XDLauncherMBeanExporter";

	public static final String NODE_PROFILE = "node";

	public static final String YARN_PROFILE = "yarn";

	private ConfigurableApplicationContext context;

	public static void main(String[] args) {
		new LauncherApplication().run(args);
	}

	public ConfigurableApplicationContext getContext() {
		return this.context;
	}

	public LauncherApplication run(String... args) {
		System.out.println(BannerUtils.displayBanner(getClass().getSimpleName(), null));

		CommandLinePropertySourceOverridingInitializer<ContainerOptions> commandLineInitializer = new CommandLinePropertySourceOverridingInitializer<ContainerOptions>(
				new ContainerOptions());

		try {

			this.context = new SpringApplicationBuilder(ContainerOptions.class, ParentConfiguration.class)
					.profiles(NODE_PROFILE, YARN_PROFILE)
					.initializers(commandLineInitializer)
					.child(LauncherApplication.class)
					.initializers(commandLineInitializer)
					.run(args);
		}
		catch (Exception e) {
			handleErrors(e);
		}
		publishContainerStarted(context);
		return this;
	}

	private void handleErrors(Exception e) {
		if (e.getCause() instanceof BindException) {
			BindException be = (BindException) e.getCause();
			for (FieldError error : be.getFieldErrors()) {
				System.err.println(String.format("the value '%s' is not allowed for property '%s'",
						error.getRejectedValue(),
						error.getField()));
				if (XDPropertyKeys.XD_CONTROL_TRANSPORT.equals(error.getField())) {
					System.err.println(
							String.format(
									"If not explicitly provided, the default value of '%s' assumes the value provided for '%s'",
									XDPropertyKeys.XD_CONTROL_TRANSPORT, XDPropertyKeys.XD_TRANSPORT));
				}
			}
		}
		else {
			e.printStackTrace();
		}
		System.exit(1);
	}

	public static void publishContainerStarted(ConfigurableApplicationContext context) {
		XDContainer container = new XDContainer();
		context.setId(container.getId());
		container.setContext(context);
		context.publishEvent(new ContainerStartedEvent(container));
	}

	@Bean
	public ApplicationListener<?> xdInitializer(ApplicationContext context) {
		XdConfigLoggingInitializer delegate = new XdConfigLoggingInitializer(true);
		delegate.setEnvironment(context.getEnvironment());
		return new SourceFilteringListener(context, delegate);
	}

	@ConditionalOnExpression("${XD_JMX_ENABLED:false}")
	@EnableMBeanExport(defaultDomain = "xd.container")
	protected static class JmxConfiguration {

		@Bean(name = MBEAN_EXPORTER_BEAN_NAME)
		public IntegrationMBeanExporter integrationMBeanExporter() {
			IntegrationMBeanExporter exporter = new IntegrationMBeanExporter();
			exporter.setDefaultDomain("xd.container");
			return exporter;
		}
	}

}
