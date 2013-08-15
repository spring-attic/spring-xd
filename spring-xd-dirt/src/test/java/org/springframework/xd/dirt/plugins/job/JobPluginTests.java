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

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.xd.module.BeanDefinitionAddingPostProcessor;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.Module;
import org.springframework.xd.module.ModuleDefinition;
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

	@Before
	public void setUp() throws Exception {
		plugin = new JobPlugin();
	}

	@Test
	public void streamPropertiesAdded() {
		Module module = new SimpleModule(new ModuleDefinition("testJob", "job"), new DeploymentMetadata("foo", 0));
		assertEquals(0, module.getProperties().size());
		plugin.preProcessModule(module);
		assertEquals(2, module.getProperties().size());
		assertEquals("foo", module.getProperties().getProperty("xd.stream.name"));
		assertEquals("true", module.getProperties().getProperty("xd.trigger.execute_on_startup"));
	}

	@Test
	public void streamComponentsAdded() {
		SimpleModule module = new SimpleModule(new ModuleDefinition("testJob", "job"), new DeploymentMetadata("foo", 0));
		plugin.preProcessModule(module);
		String[] moduleBeans = module.getApplicationContext().getBeanDefinitionNames();
		Arrays.sort(moduleBeans);
		assertEquals(2, moduleBeans.length);
		assertTrue(moduleBeans[0].contains("registrar"));
		assertTrue(moduleBeans[1].contains("startupJobLauncher"));
	}

	@Test
	public void sharedComponentsAdded() {
		GenericApplicationContext context = new GenericApplicationContext();
		plugin.postProcessSharedContext(context);
		List<BeanFactoryPostProcessor> sharedBeans = context.getBeanFactoryPostProcessors();
		assertEquals(1, sharedBeans.size());
		assertTrue(sharedBeans.get(0) instanceof BeanDefinitionAddingPostProcessor);
	}

	@Test
	public void testThatLocalCronTaskIsAdded() {
		SimpleModule module = new SimpleModule(new ModuleDefinition("testJob", "job"), new DeploymentMetadata("foo", 0));
		module.getProperties().put("cron", "*/15 * * * * *");
		plugin.preProcessModule(module);
		String[] moduleBeans = module.getApplicationContext().getBeanNamesForType(CronTask.class);
		assertEquals(1, moduleBeans.length);
		assertTrue(moduleBeans[0].contains("org.springframework.scheduling.config.CronTask"));
	}

	@Test
	public void testThatLocalFixedDelayTaskIsAdded() {
		SimpleModule module = new SimpleModule(new ModuleDefinition("testFixedDelayJob", "job"), new DeploymentMetadata("foo", 0));
		module.getProperties().put("fixedDelay", "60000");
		plugin.preProcessModule(module);
		String[] moduleBeans = module.getApplicationContext().getBeanNamesForType(IntervalTask.class);
		assertEquals(1, moduleBeans.length);
		assertTrue(moduleBeans[0].contains("org.springframework.scheduling.config.IntervalTask"));
	}

}
