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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
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
import org.springframework.xd.dirt.container.ContainerMetadata;
import org.springframework.xd.dirt.server.options.ContainerOptions;
import org.springframework.xd.dirt.server.options.XDPropertyKeys;
import org.springframework.xd.dirt.util.BannerUtils;
import org.springframework.xd.dirt.util.ConfigLocations;
import org.springframework.xd.dirt.util.XdConfigLoggingInitializer;

/**
 * The boot application class for a Container server.
 * 
 * @author Mark Fisher
 * @author David Turanski
 */
@Configuration
@EnableAutoConfiguration
@ImportResource({
	"classpath*:" + ConfigLocations.XD_CONFIG_ROOT + "plugins/*.xml"
})
public class ContainerServerApplication {

	private static final String MBEAN_EXPORTER_BEAN_NAME = "XDLauncherMBeanExporter";

	public static final String NODE_PROFILE = "node";

	private final ContainerMetadata containerMetadata;

	private ConfigurableApplicationContext coreContext;

	public ContainerServerApplication() {
		this.containerMetadata = new ContainerMetadata();
	}

	@Bean
	public ContainerMetadata containerMetadata() {
		return this.containerMetadata;
	}

	public static void main(String[] args) {
		new ContainerServerApplication().run(args);
	}

	public ConfigurableApplicationContext getContext() {
		return (ConfigurableApplicationContext) this.coreContext.getParent();
	}

	public ConfigurableApplicationContext getCoreContext() {
		return this.coreContext;
	}


	public ContainerServerApplication run(String... args) {
		System.out.println(BannerUtils.displayBanner(getClass().getSimpleName(), null));

		try {
			ContainerBootstrapContext bootstrapContext = new ContainerBootstrapContext(new ContainerOptions());

			this.coreContext = new SpringApplicationBuilder(ContainerOptions.class, ParentConfiguration.class)
					.profiles(NODE_PROFILE)
					.listeners(bootstrapContext.commandLineListener())
					.child(ContainerServerApplication.class, PropertyPlaceholderAutoConfiguration.class)
					.listeners(bootstrapContext.commandLineListener())
					.listeners(bootstrapContext.sharedContextInitializers())
					.child(CoreRuntimeConfiguration.class, PropertyPlaceholderAutoConfiguration.class)
					.listeners(bootstrapContext.commandLineListener())
					.initializers(new IdInitializer())
					.run(args);
		}
		catch (Exception e) {
			handleErrors(e);
		}
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


	private class IdInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			applicationContext.setId(containerMetadata.getId());
		}
	}
}


/**
 * Core Runtime Application Context
 * 
 * @author David Turanski
 */
@Configuration
@ImportResource({
	"classpath:" + ConfigLocations.XD_INTERNAL_CONFIG_ROOT + "container-server.xml",
})
class CoreRuntimeConfiguration {

	@Autowired
	private ContainerMetadata containerMetadata;

	@Bean
	public ApplicationListener<?> xdInitializer(ApplicationContext context) {
		XdConfigLoggingInitializer delegate = new XdConfigLoggingInitializer(true);
		delegate.setEnvironment(context.getEnvironment());
		return new SourceFilteringListener(context, delegate);
	}

	@Bean
	public ContainerRegistrar containerRegistrar() {
		return new ContainerRegistrar(containerMetadata);
	}
}
