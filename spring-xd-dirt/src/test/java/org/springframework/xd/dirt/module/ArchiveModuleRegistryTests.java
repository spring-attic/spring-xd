
package org.springframework.xd.dirt.module;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.dirt.module.ResourceModuleRegistryTests.hasName;
import static org.springframework.xd.dirt.module.ResourceModuleRegistryTests.hasType;

import java.io.File;
import java.util.List;

import org.junit.Test;

import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;


public class ArchiveModuleRegistryTests {

	private ArchiveModuleRegistry registry = new ArchiveModuleRegistry(new File("src/test/resources/boot-module-registry"));

	@Test
	public void testNotFound() {
		ModuleDefinition source = registry.findDefinition("foobar", ModuleType.source);
		assertNull(source);
	}

	@Test
	public void testFindSingle() {
		ModuleDefinition source = registry.findDefinition("file", ModuleType.source);
		assertNotNull(source);
		assertEquals("file", source.getName());
		assertEquals(ModuleType.source, source.getType());
	}

	@Test
	public void testFindByName() {
		List<ModuleDefinition> definitions = registry.findDefinitions("file");
		assertEquals(2, definitions.size());

		assertThat(definitions, hasItem(both(hasName("file")).and(hasType(ModuleType.sink))));
		assertThat(definitions, hasItem(both(hasName("file")).and(hasType(ModuleType.source))));
	}

	@Test
	public void testFindByType() {
		List<ModuleDefinition> definitions = registry.findDefinitions(ModuleType.sink);
		assertEquals(2, definitions.size());

		assertThat(definitions, hasItem(both(hasName("file")).and(hasType(ModuleType.sink))));
		assertThat(definitions, hasItem(both(hasName("log")).and(hasType(ModuleType.sink))));
	}

	@Test
	public void testFindAll() {
		List<ModuleDefinition> definitions = registry.findDefinitions();
		assertEquals(3, definitions.size());

		assertThat(definitions, hasItem(both(hasName("file")).and(hasType(ModuleType.source))));
		assertThat(definitions, hasItem(both(hasName("file")).and(hasType(ModuleType.sink))));
		assertThat(definitions, hasItem(both(hasName("log")).and(hasType(ModuleType.sink))));
	}

}
