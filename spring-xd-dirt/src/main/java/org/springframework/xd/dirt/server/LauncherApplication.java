
package org.springframework.xd.dirt.server;

import joptsimple.OptionParser;

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
import org.springframework.core.env.JOptCommandLinePropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.web.context.support.StandardServletEnvironment;
import org.springframework.xd.dirt.container.ContainerStartedEvent;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.dirt.server.options.ContainerOptions;
import org.springframework.xd.dirt.util.BannerUtils;
import org.springframework.xd.dirt.util.XdConfigLoggingInitializer;

@Configuration
@EnableAutoConfiguration
@ImportResource({
	"classpath:" + XDContainer.XD_INTERNAL_CONFIG_ROOT + "launcher.xml",
	"classpath:" + XDContainer.XD_INTERNAL_CONFIG_ROOT + "container.xml",
	"classpath*:" + XDContainer.XD_CONFIG_ROOT + "plugins/*.xml" })
public class LauncherApplication {

	public static final String NODE_PROFILE = "node";

	private ConfigurableApplicationContext context;

	public static void main(String[] args) {
		new LauncherApplication().run(args);
	}

	public ConfigurableApplicationContext getContext() {
		return this.context;
	}

	public LauncherApplication run(String... args) {
		System.out.println(BannerUtils.displayBanner(getClass().getSimpleName(), null));

		// Disable "standard" cmdline property source and use JOpt
		StandardEnvironment environment = new StandardEnvironment();
		OptionParser parser = new OptionParser();
		parser.accepts("controlTransport").withRequiredArg();
		parser.accepts("transport").withRequiredArg();
		parser.accepts("analytics").withRequiredArg();
		parser.accepts("store").withRequiredArg();
		parser.allowsUnrecognizedOptions();

		environment.getPropertySources().addFirst(new JOptCommandLinePropertySource(parser.parse(args)));

		this.context = new SpringApplicationBuilder(ContainerOptions.class, ParentConfiguration.class)
				.profiles(NODE_PROFILE)
				.environment(environment)
				.addCommandLineProperties(false)
				.child(LauncherApplication.class)
				.environment(new StandardServletEnvironment())
				.run(args);
		publishContainerStarted(context);
		return this;
	}

	public static void publishContainerStarted(ConfigurableApplicationContext context) {
		XDContainer container = new XDContainer();
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

		@Bean
		public IntegrationMBeanExporter integrationMBeanExporter() {
			IntegrationMBeanExporter exporter = new IntegrationMBeanExporter();
			exporter.setDefaultDomain("xd.container");
			return exporter;
		}
	}

}
