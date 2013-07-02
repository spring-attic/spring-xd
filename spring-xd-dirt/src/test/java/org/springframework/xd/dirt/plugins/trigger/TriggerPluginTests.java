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
package org.springframework.xd.dirt.plugins.trigger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.xd.module.Module;
import org.springframework.xd.module.SimpleModule;

/**
 *
 * @author Gunnar Hillert
 *
 */
public class TriggerPluginTests {

	private TriggerPlugin plugin;

	@Before
	public void setUp() throws Exception {
		plugin = new TriggerPlugin();
	}

	@Test
	public void streamPropertiesAdded() {
		Module module = new SimpleModule("testTrigger", "trigger");
		assertEquals(0, module.getProperties().size());

		try {
			plugin.processModule(module, "newTrigger", 0);
		} catch (IllegalArgumentException e) {
			assertEquals("The 'commonApplicationContext' property must not be null.",
				e.getMessage());
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void streamComponentsAdded() {
		SimpleModule module = new SimpleModule("testJob", "job");
		plugin.processModule(module, "foo", 0);
		String[] moduleBeans = module.getApplicationContext().getBeanDefinitionNames();
		Arrays.sort(moduleBeans);
		assertEquals(0, moduleBeans.length);
	}

	@Test
	public void sharedComponentsAdded() {
		GenericApplicationContext context = new GenericApplicationContext();
		plugin.postProcessSharedContext(context);
		List<BeanFactoryPostProcessor> sharedBeans = context.getBeanFactoryPostProcessors();
		assertEquals(0, sharedBeans.size());
	}

	@Test
	public void testTriggerAddedToSharedContext() {
		GenericApplicationContext commonContext = new GenericApplicationContext();
		plugin.postProcessSharedContext(commonContext);

		Module module = new SimpleModule("testTrigger", "trigger");
		module.getProperties().put("cron", "*/15 * * * * *");
		assertEquals(1, module.getProperties().size());
		plugin.processModule(module, "newTrigger", 0);

		CronTrigger cronTrigger = commonContext.getBean(TriggerPlugin.BEAN_NAME_PREFIX + "newTrigger", CronTrigger.class);
		assertEquals("*/15 * * * * *", cronTrigger.getExpression());
	}
}
