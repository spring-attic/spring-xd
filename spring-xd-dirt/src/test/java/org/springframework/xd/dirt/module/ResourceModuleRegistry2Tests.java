
package org.springframework.xd.dirt.module;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.dirt.module.ResourceModuleRegistryTests.hasName;
import static org.springframework.xd.dirt.module.ResourceModuleRegistryTests.hasType;

import java.util.List;

import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.junit.Test;

import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.ParentLastURLClassLoader;


/**
 * Tests for {@link ResourceModuleRegistry2}.
 * 
 * @author Eric Bottard
 */
public class ResourceModuleRegistry2Tests {


	@Test
	public void testNotFound() throws ClassNotFoundException {
		ResourceModuleRegistry2 registry = new ResourceModuleRegistry2(
				"classpath:/ResourceModuleRegistry2Tests/modules/*");
		ModuleDefinition source = registry.findDefinition("foobar", ModuleType.source);
		assertNull(source);
	}

	@Test
	public void testFindSingle() {
		ResourceModuleRegistry2 registry = new ResourceModuleRegistry2(
				"classpath:/ResourceModuleRegistry2Tests/modules/*");
		ModuleDefinition source = registry.findDefinition("foo", ModuleType.source);
		assertNotNull(source);
		assertEquals("foo", source.getName());
		assertEquals(ModuleType.source, source.getType());
		assertThat(source, canLoad("org.springframework.integration.x.splunk.SplunkTransformer"));
		assertThat(source, not(canLoad("org.springframework.integration.x.gemfire.JsonStringToObjectTransformer")));
	}

	@Test
	public void testFindByName() {
		ResourceModuleRegistry2 registry = new ResourceModuleRegistry2(
				"classpath:/ResourceModuleRegistry2Tests/modules/*");

		List<ModuleDefinition> definitions = registry.findDefinitions("foo");
		assertEquals(2, definitions.size());

		assertThat(definitions, hasItem(both(hasName("foo")).and(hasType(ModuleType.sink))));
		assertThat(definitions, hasItem(both(hasName("foo")).and(hasType(ModuleType.source))));
	}

	@Test
	public void testFindByType() {
		ResourceModuleRegistry2 registry = new ResourceModuleRegistry2(
				"classpath:/ResourceModuleRegistry2Tests/modules/*");

		List<ModuleDefinition> definitions = registry.findDefinitions(ModuleType.sink);
		assertEquals(2, definitions.size());

		assertThat(definitions, hasItem(both(hasName("foo")).and(hasType(ModuleType.sink))));
		assertThat(definitions, hasItem(both(hasName("log")).and(hasType(ModuleType.sink))));
	}

	@Test
	public void testFindAll() {
		ResourceModuleRegistry2 registry = new ResourceModuleRegistry2(
				"classpath:/ResourceModuleRegistry2Tests/modules/*");

		List<ModuleDefinition> definitions = registry.findDefinitions();
		assertEquals(3, definitions.size());

		assertThat(
				definitions,
				hasItem(both(hasName("foo")).and(hasType(ModuleType.source)).and(
						canLoad("org.springframework.integration.x.splunk.SplunkTransformer"))));
		assertThat(
				definitions,
				hasItem(both(hasName("foo")).and(hasType(ModuleType.sink)).and(
						canLoad("org.springframework.integration.x.splunk.SplunkTransformer"))));
		assertThat(
				definitions,
				hasItem(both(hasName("log")).and(hasType(ModuleType.sink)).and(
						canLoad("org.springframework.integration.x.gemfire.JsonStringToObjectTransformer"))));
	}

	Matcher<ModuleDefinition> canLoad(final String classname) {
		return new CustomMatcher<ModuleDefinition>("Can load " + classname) {

			@Override
			public boolean matches(Object item) {
				ModuleDefinition def = (ModuleDefinition) item;
				ParentLastURLClassLoader cl = new ParentLastURLClassLoader(def.getClasspath(),
						ResourceModuleRegistry2Tests.class.getClassLoader());
				try {
					Class<?> clazz = Class.forName(classname, false, cl);
					return clazz.getClassLoader() == cl;
				}
				catch (ClassNotFoundException e) {
					return false;
				}

			}
		};
	}
}
