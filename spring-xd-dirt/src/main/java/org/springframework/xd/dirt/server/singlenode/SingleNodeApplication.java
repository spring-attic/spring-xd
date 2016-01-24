/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.xd.dirt.server.singlenode;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;
import org.springframework.xd.batch.hsqldb.server.HsqlServerApplication;
import org.springframework.xd.dirt.container.decryptor.DecryptorContext;
import org.springframework.xd.dirt.container.decryptor.PropertiesDecryptor;
import org.springframework.xd.dirt.server.ApplicationUtils;
import org.springframework.xd.dirt.server.MessageBusClassLoaderFactory;
import org.springframework.xd.dirt.server.ParentConfiguration;
import org.springframework.xd.dirt.server.SharedServerContextConfiguration;
import org.springframework.xd.dirt.server.admin.AdminServerApplication;
import org.springframework.xd.dirt.server.container.ContainerBootstrapContext;
import org.springframework.xd.dirt.server.container.ContainerConfiguration;
import org.springframework.xd.dirt.server.container.ContainerServerApplication;
import org.springframework.xd.dirt.server.options.SingleNodeOptions;
import org.springframework.xd.dirt.util.BannerUtils;
import org.springframework.xd.dirt.util.XdProfiles;

/**
 * Single Node XD Runtime.
 *
 * @author Dave Syer
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 */
public class SingleNodeApplication {

	private ConfigurableApplicationContext adminContext;

	private ConfigurableApplicationContext pluginContext;

	private ConfigurableApplicationContext containerContext;

	public static void main(String[] args) {
		new SingleNodeApplication().run(args);
	}

	public SingleNodeApplication run(String... args) {

		System.out.println(BannerUtils.displayBanner(getClass().getSimpleName(), null));

		ContainerBootstrapContext bootstrapContext = new ContainerBootstrapContext(new SingleNodeOptions());

		MessageBusClassLoaderFactory classLoaderFactory = new MessageBusClassLoaderFactory();

		DecryptorContext decryptorContext = new DecryptorContext();

		SpringApplicationBuilder admin = new SpringApplicationBuilder(SingleNodeOptions.class,
				ParentConfiguration.class)
				.logStartupInfo(false)
				.listeners(bootstrapContext.commandLineListener())
				.listeners(classLoaderFactory)
				.listeners(decryptorContext.propertiesDecryptor())
				.profiles(XdProfiles.ADMIN_PROFILE, XdProfiles.SINGLENODE_PROFILE)
				.initializers(new HsqldbServerProfileActivator())
				.child(SharedServerContextConfiguration.class, SingleNodeOptions.class)
				.resourceLoader(classLoaderFactory.getResolver())
				.logStartupInfo(false)
				.listeners(bootstrapContext.commandLineListener())
				.listeners(decryptorContext.propertiesDecryptor())
				.child(SingleNodeOptions.class, AdminServerApplication.class)
				.main(AdminServerApplication.class)
				.listeners(bootstrapContext.commandLineListener())
				.listeners(decryptorContext.propertiesDecryptor());
		admin.showBanner(false);
		admin.run(args);

		SpringApplicationBuilder container = admin
				.sibling(SingleNodeOptions.class, ContainerServerApplication.class)
				.logStartupInfo(false)
				.profiles(XdProfiles.CONTAINER_PROFILE, XdProfiles.SINGLENODE_PROFILE)
				.listeners(ApplicationUtils.mergeApplicationListeners(bootstrapContext.commandLineListener(),
						bootstrapContext.pluginContextInitializers()))
				.listeners(decryptorContext.propertiesDecryptor())
				.child(ContainerConfiguration.class)
				.main(ContainerServerApplication.class)
				.listeners(bootstrapContext.commandLineListener())
				.listeners(decryptorContext.propertiesDecryptor())
				.web(false);
		container.showBanner(false);
		container.run(args);

		adminContext = admin.context();

		containerContext = container.context();
		pluginContext = (ConfigurableApplicationContext) containerContext.getParent();

		return this;
	}

	public void close() {
		close(containerContext);
		close(pluginContext);
		close(adminContext);
	}

	private void close(ConfigurableApplicationContext context) {
		if (context != null) {
			context.close();
			ApplicationContext parent = context.getParent();
			if (parent instanceof ConfigurableApplicationContext) {
				close((ConfigurableApplicationContext) parent);
			}
		}
	}

	public ConfigurableApplicationContext adminContext() {
		return adminContext;
	}

	public ConfigurableApplicationContext pluginContext() {
		return pluginContext;
	}

	public ConfigurableApplicationContext containerContext() {
		return containerContext;
	}

	/**
	 * Initializer class that activates {@link HsqlServerApplication.HSQLDBSERVER_PROFILE} if the underlying datasource
	 * is hsql and embedded hsqldb is opted.
	 */
	class HsqldbServerProfileActivator implements
			ApplicationContextInitializer<ConfigurableApplicationContext> {

		private static final String SPRING_DATASOURCE_URL_OPTION = "${spring.datasource.url}";

		private static final String SINGLENODE_EMBEDDED_HSQL = "${embeddedHsql}";

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			String dataSourceUrl = applicationContext.getEnvironment()
					.resolvePlaceholders(SPRING_DATASOURCE_URL_OPTION);
			String embeddedHsql = applicationContext.getEnvironment().resolvePlaceholders(SINGLENODE_EMBEDDED_HSQL);
			Assert.notNull(dataSourceUrl, "At least one datasource (for batch) must be set.");
			Assert.notNull(embeddedHsql,
					"The property singlenode.embeddedHsql must be set for singlenode. Default is true.");
			if (dataSourceUrl.contains("hsql") && embeddedHsql.equals("true")) {
				applicationContext.getEnvironment().addActiveProfile(HsqlServerApplication.HSQLDBSERVER_PROFILE);
			}
		}
	}

}
