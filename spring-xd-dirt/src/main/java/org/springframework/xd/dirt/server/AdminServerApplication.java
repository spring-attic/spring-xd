
package org.springframework.xd.dirt.server;

import javax.servlet.Filter;

import joptsimple.OptionParser;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.event.SourceFilteringListener;
import org.springframework.core.env.JOptCommandLinePropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.web.context.support.StandardServletEnvironment;
import org.springframework.web.filter.HttpPutFormContentFilter;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.dirt.rest.RestConfiguration;
import org.springframework.xd.dirt.server.options.AdminOptions;
import org.springframework.xd.dirt.util.BannerUtils;
import org.springframework.xd.dirt.util.XdConfigLoggingInitializer;

@Configuration
@EnableAutoConfiguration
@ImportResource("classpath:" + XDContainer.XD_INTERNAL_CONFIG_ROOT
		+ "admin-server.xml")
@Import(RestConfiguration.class)
public class AdminServerApplication {

	public static final String ADMIN_PROFILE = "adminServer";

	public static final String HSQL_PROFILE = "hsqldb";

	private ConfigurableApplicationContext context;

	public static void main(String[] args) {
		new AdminServerApplication().run(args);
	}

	public ConfigurableApplicationContext getContext() {
		return this.context;
	}

	public AdminServerApplication run(String... args) {
		System.out.println(BannerUtils.displayBanner(getClass().getSimpleName(), null));

		// Disable "standard" cmdline property source and use JOpt
		StandardEnvironment environment = new StandardEnvironment();
		OptionParser parser = new OptionParser();
		parser.accepts("controlTransport").withRequiredArg();
		parser.accepts("analytics").withRequiredArg();
		parser.accepts("store").withRequiredArg();

		environment.getPropertySources().addFirst(new JOptCommandLinePropertySource(parser.parse(args)));

		this.context = new SpringApplicationBuilder(AdminOptions.class, ParentConfiguration.class)
				.profiles(ADMIN_PROFILE)
				.addCommandLineProperties(false)
				.environment(environment)
				.child(AdminServerApplication.class)
				.environment(new StandardServletEnvironment())
				.run(args);
		return this;
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

	@ConditionalOnExpression("${XD_JMX_ENABLED:false}")
	@EnableMBeanExport(defaultDomain = "xd.admin")
	protected static class JmxConfiguration {

		@Bean
		public IntegrationMBeanExporter integrationMBeanExporter() {
			IntegrationMBeanExporter exporter = new IntegrationMBeanExporter();
			exporter.setDefaultDomain("xd.admin");
			return exporter;
		}
	}

}
