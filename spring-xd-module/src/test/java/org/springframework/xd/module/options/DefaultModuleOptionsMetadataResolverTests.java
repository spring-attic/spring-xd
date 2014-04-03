
package org.springframework.xd.module.options;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.module.ModuleType.source;
import static org.springframework.xd.module.options.ModuleOptionMatchers.moduleOptionNamed;

import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.xd.module.ModuleDefinition;


/**
 * Tests for {@link DefaultModuleOptionsMetadataResolver}.
 * 
 * @author Eric Bottard
 */
public class DefaultModuleOptionsMetadataResolverTests {

	private DefaultModuleOptionsMetadataResolver metadataResolver = new DefaultModuleOptionsMetadataResolver();

	@Test
	@SuppressWarnings("unchecked")
	public void testPojoOptionsConstruction() {
		Resource resource = new ClassPathResource(
				"/DefaultModuleOptionsMetadataResolverTests-modules/source/module1/config/module1.xml");
		ModuleDefinition definition = new ModuleDefinition("module1", source, resource);
		ModuleOptionsMetadata metadata = metadataResolver.resolve(definition);
		assertThat(
				metadata,
				containsInAnyOrder(moduleOptionNamed("bar"), moduleOptionNamed("foo")));

	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMixin() {
		Resource resource = new ClassPathResource(
				"/DefaultModuleOptionsMetadataResolverTests-modules/source/module2/config/module2.xml");
		ModuleDefinition definition = new ModuleDefinition("module2", source, resource);
		ModuleOptionsMetadata metadata = metadataResolver.resolve(definition);
		assertThat(
				metadata,
				containsInAnyOrder(moduleOptionNamed("bar"), moduleOptionNamed("fizz"), moduleOptionNamed("foo"),
						moduleOptionNamed("optionDefinedHere")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMixinOverlap() {
		Resource resource = new ClassPathResource(
				"/DefaultModuleOptionsMetadataResolverTests-modules/source/module3/config/module3.xml");
		ModuleDefinition definition = new ModuleDefinition("module3", source, resource);
		metadataResolver.resolve(definition);
	}
}
