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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.xd.dirt.server.AdminServer;
import org.springframework.xd.dirt.server.options.XDPropertyKeys;
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

	@BeforeClass
	public static void init() {
		System.setProperty(XDPropertyKeys.XD_HOME, "..");
	}

	@AfterClass
	public static void after() {
		System.clearProperty(XDPropertyKeys.XD_HOME);
	}

	@Before
	public void setUp() throws Exception {

		plugin = new JobPlugin();
		sharedContext = new ClassPathXmlApplicationContext("classpath:/META-INF/spring-xd/batch/batch.xml");
		sharedContext.getEnvironment().setActiveProfiles(AdminServer.ADMIN_PROFILE);
		Properties hsqlProperties = new Properties();
		hsqlProperties.put("hsql.server.dbname", "test");
		hsqlProperties.put("hsql.server.port", "9100");
		System.setProperty("hsql.server.database", "xdjobrepotest");
		sharedContext.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource(
				"hsqlProperties", hsqlProperties));
		plugin.preProcessSharedContext(sharedContext);

	}

	@Test
	public void streamPropertiesAdded() {
		Module module = new SimpleModule(new ModuleDefinition("testJob", ModuleType.job), new DeploymentMetadata("foo",
				0));
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

		SimpleModule module = new SimpleModule(new ModuleDefinition("testJob", ModuleType.job), new DeploymentMetadata(
				"foo", 0));

		GenericApplicationContext context = new GenericApplicationContext();
		plugin.preProcessModule(module);
		plugin.preProcessSharedContext(context);

		String[] moduleBeans = module.getApplicationContext().getBeanDefinitionNames();
		Arrays.sort(moduleBeans);

		SortedSet<String> names = new TreeSet<String>();
		names.addAll(Arrays.asList(moduleBeans));

		assertTrue(names.size() > 8);

		assertTrue(names.contains("registrar"));
		assertTrue(names.contains("jobFactoryBean"));
		assertTrue(names.contains("jobLaunchRequestTransformer"));
		assertTrue(names.contains("jobLaunchingMessageHandler"));
		assertTrue(names.contains("input"));
		assertTrue(names.contains("jobLaunchingChannel"));
		assertTrue(names.contains("notifications"));
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
