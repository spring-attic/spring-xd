
package org.springframework.xd.dirt.server;

import javax.servlet.Filter;

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
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.web.filter.HttpPutFormContentFilter;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.dirt.rest.RestConfiguration;
import org.springframework.xd.dirt.server.options.AdminOptions;
import org.springframework.xd.dirt.server.options.CommandLinePropertySourceOverridingInitializer;
import org.springframework.xd.dirt.util.BannerUtils;
import org.springframework.xd.dirt.util.XdConfigLoggingInitializer;

@Configuration
@EnableAutoConfiguration
@ImportResource("classpath:" + XDContainer.XD_INTERNAL_CONFIG_ROOT
		+ "admin-server.xml")
@Import(RestConfiguration.class)
public class AdminServerApplication {

	private static final String MBEAN_EXPORTER_BEAN_NAME = "XDAdminMBeanExporter";

	public static final String ADMIN_PROFILE = "adminServer";

	public static final String YARN_PROFILE = "yarn";

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

		CommandLinePropertySourceOverridingInitializer<AdminOptions> commandLineInitializer = new CommandLinePropertySourceOverridingInitializer<AdminOptions>(
				new AdminOptions());

		this.context = new SpringApplicationBuilder(AdminOptions.class, ParentConfiguration.class)
				.profiles(ADMIN_PROFILE, YARN_PROFILE)
				.initializers(commandLineInitializer)
				.child(AdminServerApplication.class)
				.initializers(commandLineInitializer)
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

		@Bean(name = MBEAN_EXPORTER_BEAN_NAME)
		public IntegrationMBeanExporter integrationMBeanExporter() {
			IntegrationMBeanExporter exporter = new IntegrationMBeanExporter();
			exporter.setDefaultDomain("xd.admin");
			return exporter;
		}
	}

}
