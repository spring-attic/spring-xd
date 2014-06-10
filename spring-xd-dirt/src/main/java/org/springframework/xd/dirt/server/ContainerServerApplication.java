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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.event.SourceFilteringListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.xd.dirt.container.ContainerAttributes;
import org.springframework.xd.dirt.container.store.ContainerAttributesRepository;
import org.springframework.xd.dirt.job.JobFactory;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDeployer;
import org.springframework.xd.dirt.server.options.ContainerOptions;
import org.springframework.xd.dirt.stream.JobDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamFactory;
import org.springframework.xd.dirt.util.BannerUtils;
import org.springframework.xd.dirt.util.ConfigLocations;
import org.springframework.xd.dirt.util.RuntimeUtils;
import org.springframework.xd.dirt.util.XdConfigLoggingInitializer;
import org.springframework.xd.dirt.util.XdProfiles;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnectionConfigurer;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;

/**
 * The boot application class for a Container server.
 *
 * @author Mark Fisher
 * @author David Turanski
 */
@Configuration
@EnableAutoConfiguration(exclude = { BatchAutoConfiguration.class, JmxAutoConfiguration.class })
@Import(PropertyPlaceholderAutoConfiguration.class)
public class ContainerServerApplication implements EnvironmentAware {

	private static final Log logger = LogFactory.getLog(ContainerServerApplication.class);

	private static final String CONTAINER_ATTRIBUTES_PREFIX = "xd.container.";

	private ConfigurableApplicationContext containerContext;

	private ConfigurableEnvironment environment;

	public static void main(String[] args) {
		new ContainerServerApplication().run(args);
	}

	// TODO: Is this needed anymore?
	public ConfigurableApplicationContext getPluginContext() {
		if (this.containerContext != null) {
			return (ConfigurableApplicationContext) this.containerContext.getParent();
		}
		else {
			logger.error("The container has not been initialized yet.");
			return null;
		}
	}

	public ConfigurableApplicationContext getContainerContext() {
		return this.containerContext;
	}

	public void dumpContextConfiguration() {
		ApplicationUtils.dumpContainerApplicationContextConfiguration(this.containerContext);
	}

	public ContainerServerApplication run(String... args) {
		System.out.println(BannerUtils.displayBanner(getClass().getSimpleName(), null));

		try {
			ContainerBootstrapContext bootstrapContext = new ContainerBootstrapContext(new ContainerOptions());

			this.containerContext = new SpringApplicationBuilder(ContainerOptions.class, ParentConfiguration.class)
					.profiles(XdProfiles.CONTAINER_PROFILE)
					.listeners(bootstrapContext.commandLineListener())
					.child(SharedServerContextConfiguration.class, ContainerOptions.class)
					.listeners(bootstrapContext.commandLineListener())
					.child(ContainerServerApplication.class)
					.listeners(
							ApplicationUtils.mergeApplicationListeners(bootstrapContext.commandLineListener(),
									bootstrapContext.pluginContextInitializers()))
					.child(ContainerConfiguration.class)
					.listeners(bootstrapContext.commandLineListener())
					.initializers(new IdInitializer()).run(args);
		}
		catch (Exception e) {
			handleFieldErrors(e);
		}
		return this;
	}

	@Bean
	public ContainerAttributes containerAttributes() {
		ContainerAttributes containerAttributes = new ContainerAttributes();
		containerAttributes.setHost(RuntimeUtils.getHost()).setIp(RuntimeUtils.getIpAddress()).setPid(
				RuntimeUtils.getPid());
		setConfiguredContainerAttributes(containerAttributes);
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

	private class IdInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			ContainerAttributes containerAttributes = applicationContext.getParent().getBean(ContainerAttributes.class);
			applicationContext.setId(containerAttributes.getId());
		}
	}
}


/**
 * Container Application Context
 *
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 */
@Configuration
@ImportResource({ "classpath:" + ConfigLocations.XD_INTERNAL_CONFIG_ROOT + "container-server.xml", })
@EnableAutoConfiguration(exclude = { BatchAutoConfiguration.class, JmxAutoConfiguration.class })
class ContainerConfiguration {

	@Autowired
	private ContainerAttributes containerAttributes;

	@Autowired
	private ContainerAttributesRepository containerAttributesRepository;

	@Autowired
	private StreamDefinitionRepository streamDefinitionRepository;

	@Autowired
	private JobDefinitionRepository jobDefinitionRepository;

	@Autowired
	private ModuleDefinitionRepository moduleDefinitionRepository;

	@Autowired
	private ModuleOptionsMetadataResolver moduleOptionsMetadataResolver;

	@Autowired
	private ModuleDeployer moduleDeployer;

	@Autowired
	private ZooKeeperConnection zooKeeperConnection;

	/*
	 * An optional bean to configure the ZooKeeperConnection. XD by default does not provide this bean but it may be
	 * added via an extension. This is also effected by the boolean property value ${zk.client.connection.configured}
	 * which if set, defers the start of the ZooKeeper connection until now.
	 */
	@Autowired(required = false)
	ZooKeeperConnectionConfigurer zooKeeperConnectionConfigurer;

	@Bean
	public ApplicationListener<?> xdInitializer(ApplicationContext context) {
		XdConfigLoggingInitializer delegate = new XdConfigLoggingInitializer(true);
		delegate.setEnvironment(context.getEnvironment());
		return new SourceFilteringListener(context, delegate);
	}

	@Bean
	public ContainerRegistrar containerRegistrar() {
		if (zooKeeperConnectionConfigurer != null) {
			zooKeeperConnectionConfigurer.configureZooKeeperConnection(zooKeeperConnection);
			zooKeeperConnection.start();
		}

		StreamFactory streamFactory = new StreamFactory(streamDefinitionRepository, moduleDefinitionRepository,
				moduleOptionsMetadataResolver);

		JobFactory jobFactory = new JobFactory(jobDefinitionRepository, moduleDefinitionRepository,
				moduleOptionsMetadataResolver);

		return new ContainerRegistrar(zooKeeperConnection, containerAttributes,
				containerAttributesRepository,
				streamFactory,
				jobFactory,
				moduleOptionsMetadataResolver,
				moduleDeployer);
	}
}
