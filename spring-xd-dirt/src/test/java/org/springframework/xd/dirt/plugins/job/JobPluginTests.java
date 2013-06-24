package org.springframework.xd.dirt.plugins.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.xd.dirt.plugins.BeanDefinitionAddingPostProcessor;
import org.springframework.xd.module.Module;
import org.springframework.xd.module.SimpleModule;

public class JobPluginTests {

	private JobPlugin plugin;

	@Before
	public void setUp() throws Exception {
		plugin = new JobPlugin();
	}

	@Test
	public void streamPropertiesAdded() {
		Module module = new SimpleModule("testJob", "job");
		assertEquals(0, module.getProperties().size());
		plugin.processModule(module, "foo", 0);
		assertEquals(1, module.getProperties().size());
		assertEquals("foo", module.getProperties().getProperty("xd.stream.name"));
	}

	@Test
	public void streamComponentsAdded() {
		SimpleModule module = new SimpleModule("testJob", "job");
		plugin.processModule(module, "foo", 0);
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
}
