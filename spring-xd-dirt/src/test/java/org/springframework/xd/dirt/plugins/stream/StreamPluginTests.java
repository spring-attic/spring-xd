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

package org.springframework.xd.dirt.plugins.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.xd.dirt.plugins.BeanDefinitionAddingPostProcessor;
import org.springframework.xd.module.Module;
import org.springframework.xd.module.SimpleModule;

/**
 * @author Mark Fisher
 * @author Jennifer Hickey
 */
public class StreamPluginTests {

	private StreamPlugin plugin = new StreamPlugin();

	@Test
	public void streamPropertiesAdded() {
		Module module = new SimpleModule("testsource", "source");
		assertEquals(0, module.getProperties().size());
		plugin.processModule(module, "foo", 0);
		assertEquals(2, module.getProperties().size());
		assertEquals("foo", module.getProperties().getProperty("xd.stream.name"));
		assertEquals("0", module.getProperties().getProperty("xd.module.index"));
	}

	@Test
	public void streamComponentsAdded() {
		SimpleModule module = new SimpleModule("testsource", "source");
		plugin.processModule(module, "mystream", 1);
		String[] moduleBeans = module.getApplicationContext().getBeanDefinitionNames();
		assertEquals(1, moduleBeans.length);
		assertTrue(moduleBeans[0].contains("ChannelRegistrar#"));
	}

	@Test
	public void tapComponentsAdded() {
		SimpleModule module = new SimpleModule("tap", "source");
		plugin.processModule(module, "mystream", 1);
		String[] moduleBeans = module.getApplicationContext().getBeanDefinitionNames();
		assertEquals(2, moduleBeans.length);
		assertTrue(moduleBeans[0].contains("ChannelRegistrar#"));
		assertTrue(moduleBeans[1].contains("Tap#"));
	}

	@Test
	public void sharedComponentsAdded() {
		GenericApplicationContext context = new GenericApplicationContext();
		plugin.postProcessSharedContext(context);
		List<BeanFactoryPostProcessor> sharedBeans = context.getBeanFactoryPostProcessors();
		assertEquals(1, sharedBeans.size());
		assertTrue(sharedBeans.get(0) instanceof BeanDefinitionAddingPostProcessor);
	}

}
