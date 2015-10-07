/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.extensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.integration.codec.kryo.AbstractKryoRegistrar;
import org.springframework.integration.codec.kryo.CompositeKryoRegistrar;
import org.springframework.integration.codec.kryo.KryoClassListRegistrar;
import org.springframework.integration.codec.kryo.PojoCodec;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.util.MimeType;
import org.springframework.xd.dirt.container.initializer.AbstractResourceBeanDefinitionProvider;
import org.springframework.xd.dirt.integration.bus.converter.CompositeMessageConverterFactory;
import org.springframework.xd.dirt.plugins.stream.ModuleTypeConversionPlugin;
import org.springframework.xd.dirt.server.TestApplicationBootstrap;
import org.springframework.xd.dirt.server.singlenode.SingleNodeApplication;
import org.springframework.xd.extensions.test.Bar;
import org.springframework.xd.extensions.test.Foo;
import org.springframework.xd.extensions.test.StubPojoToStringConverter;
import org.springframework.xd.tuple.DefaultTuple;

import com.esotericsoftware.kryo.Registration;

import spring.xd.bus.ext.CustomKryoRegistrarConfig;

/**
 * @author David Turanski
 */
public class ExtensionsTests {

	private static SingleNodeApplication application;

	private static ApplicationContext context;

	private static String originalConfigLocation;

	@BeforeClass
	public static void setUp() {
		originalConfigLocation = System.getProperty("spring.config.location");
		System.setProperty("spring.config.location", "classpath:extensions/extensions-test.yml");
		application = new TestApplicationBootstrap().getSingleNodeApplication().run();
		context = application.pluginContext();
	}

	@AfterClass
	public static void close() {
		if (application != null) {
			application.close();
		}
	}

	@Test
	public void addBeans() {
		context.getBean("javaConfigExtension");
		context.getBean("xmlExtension");
	}

	@Test
	public void extensionsProperties() throws IOException {
		Object extensionsBasePackage = context.getEnvironment().getProperty("xd.extensions.basepackages",
				Object.class);
		assertEquals("org.springframework.xd.extensions.test,org.springframework.xd.extensions.test2",
				extensionsBasePackage);
		String extensionsLocation = context.getEnvironment().getProperty("xd.extensions.locations");
		assertEquals("META-INF/spring-xd/ext/test,META-INF/spring-xd/ext/test2",
				extensionsLocation);
	}

	@Test
	public void addCustomConverter() {
		List<?> customMessageConverters = context.getBean("customMessageConverters", List.class);

		assertEquals(1, customMessageConverters.size());

		ModuleTypeConversionPlugin plugin = context.getBean(ModuleTypeConversionPlugin.class);

		CompositeMessageConverterFactory factory = (CompositeMessageConverterFactory) TestUtils.getPropertyValue(
				plugin, "converterFactory");
		CompositeMessageConverter converters = factory.newInstance(MimeType.valueOf("application/x-xd-foo"));
		assertEquals(1, converters.getConverters().size());
		assertTrue(converters.getConverters().iterator().next() instanceof StubPojoToStringConverter);
	}

	@Test
	public void resolveResourceLocations() {
		TestResourceBeanDefinitionProvider beanDefinitionProvider = new TestResourceBeanDefinitionProvider("foo,bar");
		String[] resolvedLocations = beanDefinitionProvider.resolveLocations();
		assertEquals("classpath*:foo/**/*.*", resolvedLocations[0]);
		assertEquals("classpath*:bar/**/*.*", resolvedLocations[1]);

		beanDefinitionProvider = new TestResourceBeanDefinitionProvider("classpath:foo/");
		resolvedLocations = beanDefinitionProvider.resolveLocations();
		assertEquals("classpath:foo/**/*.*", resolvedLocations[0]);

		beanDefinitionProvider = new TestResourceBeanDefinitionProvider("classpath*:foo/");
		resolvedLocations = beanDefinitionProvider.resolveLocations();
		assertEquals("classpath*:foo/**/*.*", resolvedLocations[0]);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void customKryoCRegistrars() {


		Map<String, AbstractKryoRegistrar> kryoRegistrarMap = context.getParent().getBeansOfType(AbstractKryoRegistrar
				.class);
		// Two default, two custom (see META-INF/spring-xd/bus/ext/custom-types.xml
		// and spring.xd.bus.ext.CustomKryoRegistarConfig
		assertEquals(4, kryoRegistrarMap.size());

		AbstractKryoRegistrar registrar = context.getBean(KryoClassListRegistrar.class);
		assertEquals(2,registrar.getRegistrations().size());
		List<Class<?>> classes = Arrays.asList(new Class<?>[] {Foo.class, Bar.class});
		for (Registration registration: registrar.getRegistrations()) {
			assertTrue(classes.contains(registration.getType()));
		}

		PojoCodec pojoCodec = context.getBean("codec", PojoCodec.class);

		CompositeKryoRegistrar compositeKryoRegistrar = (CompositeKryoRegistrar) TestUtils.getPropertyValue(pojoCodec,
				"kryoRegistrar");
		List<Registration> registrations = compositeKryoRegistrar.getRegistrations();
		//3 from default + 3 from extended
		List<Class<?>> allClasses = Arrays.asList(new Class<?>[] {Foo.class, Bar.class, CustomKryoRegistrarConfig
				.MyObject.class, DefaultTuple.class, ArrayList.class, File.class});

		assertEquals(allClasses.size(), registrations.size());
		for (Registration registration:registrations) {
			assertTrue(allClasses.contains(registration.getType()));
		}

	}

	@AfterClass
	public static void tearDown() {
		if (originalConfigLocation != null) {
			System.setProperty("spring.config.location", originalConfigLocation);
		}
		else {
			System.clearProperty("spring.config.location");
		}
	}

	/*
	 * To test the protected method in the base class
	 */
	static class TestResourceBeanDefinitionProvider extends AbstractResourceBeanDefinitionProvider {

		private String locations;

		TestResourceBeanDefinitionProvider(String locations) {
			this.locations = locations;
		}

		@Override
		public int getOrder() {
			return 0;
		}

		@Override
		protected String getExtensionsLocations() {
			return locations;
		}

		public String[] resolveLocations() {
			return getLocations();
		}
	}
}
