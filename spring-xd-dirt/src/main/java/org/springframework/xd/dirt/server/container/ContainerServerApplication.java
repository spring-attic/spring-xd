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

package org.springframework.xd.dirt.server.container;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.actuate.autoconfigure.AuditAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.xd.dirt.cluster.ContainerAttributes;
import org.springframework.xd.dirt.container.decryptor.DecryptorContext;
import org.springframework.xd.dirt.container.decryptor.PropertiesDecryptor;
import org.springframework.xd.dirt.server.ApplicationUtils;
import org.springframework.xd.dirt.server.MessageBusClassLoaderFactory;
import org.springframework.xd.dirt.server.ParentConfiguration;
import org.springframework.xd.dirt.server.SharedServerContextConfiguration;
import org.springframework.xd.dirt.server.options.ContainerOptions;
import org.springframework.xd.dirt.util.BannerUtils;
import org.springframework.xd.dirt.util.RuntimeUtils;
import org.springframework.xd.dirt.util.XdProfiles;

/**
 * The boot application class for a Container server.
 *
 * @author Mark Fisher
 * @author David Turanski
 * @author Marius Bogoevici
 * @author Gunnar Hillert
 *
 */
@Configuration
@EnableAutoConfiguration(exclude = { BatchAutoConfiguration.class, JmxAutoConfiguration.class,
		AuditAutoConfiguration.class, MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
		SolrAutoConfiguration.class })
public class ContainerServerApplication implements EnvironmentAware {

	private static final Logger logger = LoggerFactory.getLogger(ContainerServerApplication.class);

	public static final String CONTAINER_ATTRIBUTES_PREFIX = "xd.container.";

	private ConfigurableApplicationContext containerContext;

	private ConfigurableEnvironment environment;

	public static void main(String[] args) {
		new ContainerServerApplication().run(args);
	}

	public void dumpContextConfiguration() {
		ApplicationUtils.dumpContainerApplicationContextConfiguration(this.containerContext);
	}

	public ContainerServerApplication run(String... args) {
		System.out.println(BannerUtils.displayBanner(getClass().getSimpleName(), null));

		try {
			ContainerBootstrapContext bootstrapContext = new ContainerBootstrapContext(new ContainerOptions());

			MessageBusClassLoaderFactory classLoaderFactory = new MessageBusClassLoaderFactory();

			DecryptorContext decryptorContext = new DecryptorContext();

			this.containerContext = new SpringApplicationBuilder(ContainerOptions.class, ParentConfiguration.class)
					.logStartupInfo(false)
					.profiles(XdProfiles.CONTAINER_PROFILE)
					.listeners(bootstrapContext.commandLineListener())
					.listeners(classLoaderFactory)
					.listeners(decryptorContext.propertiesDecryptor())
					.child(SharedServerContextConfiguration.class, ContainerOptions.class)
					.resourceLoader(classLoaderFactory.getResolver())
					.logStartupInfo(false)
					.listeners(bootstrapContext.commandLineListener())
					.listeners(decryptorContext.propertiesDecryptor())
					.child(ContainerServerApplication.class)
					.logStartupInfo(false)
					.listeners(
							ApplicationUtils.mergeApplicationListeners(bootstrapContext.commandLineListener(),
									bootstrapContext.pluginContextInitializers()))
					.child(ContainerConfiguration.class)
					.listeners(bootstrapContext.commandLineListener())
					.listeners(decryptorContext.propertiesDecryptor())
					.initializers(new IdInitializer())
					.showBanner(false)
					.run(args);
		}
		catch (Exception e) {
			handleFieldErrors(e);
		}
		return this;
	}

	@Bean
	public ContainerAttributes containerAttributes() {

		final ContainerAttributes containerAttributes = new ContainerAttributes();
		setConfiguredContainerAttributes(containerAttributes);

		final String containerIp = environment.getProperty(CONTAINER_ATTRIBUTES_PREFIX
				+ ContainerAttributes.IP_ADDRESS_KEY);
		final String containerHostname = environment.getProperty(CONTAINER_ATTRIBUTES_PREFIX
				+ ContainerAttributes.HOST_KEY);

		containerAttributes.setIp(StringUtils.hasText(containerIp) ? containerIp : RuntimeUtils.getIpAddress());
		containerAttributes.setHost(StringUtils.hasText(containerHostname) ? containerHostname : RuntimeUtils.getHost());

		containerAttributes.setPid(RuntimeUtils.getPid());
		return containerAttributes;
	}

	/**
	 * @param containerAttributes
	 */
	private void setConfiguredContainerAttributes(ContainerAttributes containerAttributes) {
		Map<String, String> attributes = new HashMap<String, String>();
		for (PropertySource<?> propertySource : environment.getPropertySources()) {
			if (propertySource instanceof EnumerablePropertySource) {
				EnumerablePropertySource<?> ps = (EnumerablePropertySource<?>) propertySource;
				for (String key : ps.getPropertyNames()) {
					if (key.startsWith(CONTAINER_ATTRIBUTES_PREFIX)) {
						String attributeKey = key.replaceAll(CONTAINER_ATTRIBUTES_PREFIX, "");
						attributes.put(attributeKey, environment.getProperty(key));
					}
				}
			}
		}
		containerAttributes.putAll(attributes);
	}

	@Override
	public void setEnvironment(Environment environment) {
		Assert.isInstanceOf(ConfigurableEnvironment.class, environment,
				"unsupported environment type. " + environment.getClass());
		this.environment = (ConfigurableEnvironment) environment;
	}

	private void handleFieldErrors(Exception e) {
		if (e.getCause() instanceof BindException) {
			BindException be = (BindException) e.getCause();
			for (FieldError error : be.getFieldErrors()) {
				logger.error(String.format("the value '%s' is not allowed for property '%s'",
						error.getRejectedValue(),
						error.getField()));
			}
		}
		else {
			e.printStackTrace();
		}
		System.exit(1);
	}

	private static class IdInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			ContainerAttributes containerAttributes = applicationContext.getParent().getBean(ContainerAttributes.class);
			applicationContext.setId(containerAttributes.getId());
		}
	}
}
