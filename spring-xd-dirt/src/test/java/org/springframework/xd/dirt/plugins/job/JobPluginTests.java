/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.plugins.job;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.xd.dirt.server.AdminServerApplication;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.Module;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.SimpleModule;

/**
 * 
 * @author Michael Minella
 * @author Gunnar Hillert
 * @author Gary Russell
 * 
 */
public class JobPluginTests {

	private JobPlugin plugin;

	private ConfigurableApplicationContext sharedContext;

	@After
	public void tearDown() {
		if (sharedContext != null) {
			sharedContext.close();
		}
	}

	@Configuration
	@ImportResource("classpath:/META-INF/spring-xd/batch/batch.xml")
	@EnableAutoConfiguration
	@EnableBatchProcessing
	public static class SharedConfiguration {

	}

	@Before
	public void setUp() throws Exception {

		plugin = new JobPlugin();
		sharedContext = new SpringApplicationBuilder(SharedConfiguration.class).profiles(
				AdminServerApplication.ADMIN_PROFILE, AdminServerApplication.HSQL_PROFILE).properties(
				"spring.datasource.url=jdbc:hsqldb:mem:xdjobrepotest") //
		.web(false).initializers(
				new ApplicationContextInitializer<ConfigurableApplicationContext>() {

					@Override
					public void initialize(ConfigurableApplicationContext applicationContext) {
						plugin.preProcessSharedContext(applicationContext);
					}

				}).run();

	}

	@Test
	public void streamPropertiesAdded() {
		Module module = new SimpleModule(new ModuleDefinition("testJob", ModuleType.job),
				new DeploymentMetadata(
						"foo", 0));

		assertEquals(0, module.getProperties().size());
		plugin.preProcessModule(module);

		Properties moduleProperties = module.getProperties();

		assertEquals(4, moduleProperties.size());
		assertEquals("foo", moduleProperties.getProperty("xd.stream.name"));
		assertEquals("", moduleProperties.getProperty("dateFormat"));
		assertEquals("", moduleProperties.getProperty("numberFormat"));
		assertEquals("true", moduleProperties.getProperty("makeUnique"));
	}

	@Test
	public void streamComponentsAdded() {

		Module module = Mockito.mock(Module.class);
		Mockito.when(module.getType()).thenReturn(ModuleType.job);
		Properties properties = new Properties();
		Mockito.when(module.getProperties()).thenReturn(properties);
		Mockito.when(module.getDeploymentMetadata()).thenReturn(new DeploymentMetadata("job", 0));

		GenericApplicationContext context = new GenericApplicationContext();
		plugin.preProcessModule(module);
		plugin.preProcessSharedContext(context);

		Mockito.verify(module).addComponents(Matchers.any(Resource.class));

		// TODO: assert that the right resource was added.
		// assertTrue(names.contains("registrar"));
		// assertTrue(names.contains("jobFactoryBean"));
		// assertTrue(names.contains("jobLaunchRequestTransformer"));
		// assertTrue(names.contains("jobLaunchingMessageHandler"));
		// assertTrue(names.contains("input"));
		// assertTrue(names.contains("jobLaunchingChannel"));
		// assertTrue(names.contains("notifications"));
	}

	/**
	 * There should not be any shared beans for the plugin. As per XD-703 the common job beans are registered in the
	 * global common context, so that they are shared across xd-admin/xd-container.
	 */
	@Test
	public void sharedComponentsAdded() {
		GenericApplicationContext context = new GenericApplicationContext();
		plugin.preProcessSharedContext(context);
		List<BeanFactoryPostProcessor> sharedBeans = context.getBeanFactoryPostProcessors();
		assertEquals(0, sharedBeans.size());
	}

}
