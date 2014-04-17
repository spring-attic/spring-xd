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

package org.springframework.xd.module.options;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.module.options.ModuleOptionMatchers.moduleOptionNamed;
import static org.springframework.xd.module.options.ModuleOptionMatchers.moduleOptionWithDefaultValue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;


/**
 * Tests for {@link DefaultModuleOptionsMetadataCollector}.
 * 
 * @author Eric Bottard
 */
public class DefaultModuleOptionsMetadataCollectorTests {

	@Rule
	public TestName name = new TestName();

	private ModuleOptionsMetadata result;

	@Before
	public void setUp() {
		final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
		Resource resource = new ClassPathResource("/" + getClass().getSimpleName() + "/" + name.getMethodName()
				+ ".xml");
		reader.loadBeanDefinitions(resource);

		DefaultModuleOptionsMetadataCollector collector = new DefaultModuleOptionsMetadataCollector();
		result = collector.collect(beanFactory);
	}

	@Test
	public void testOptionNameCollection() {
		assertThat(result, hasItem(moduleOptionNamed("post")));
	}

	@Test
	public void testDefaultValueCollection() {
		assertThat(result,
				hasItem(allOf(moduleOptionNamed("post"), moduleOptionWithDefaultValue(equalTo("foo")))));
	}

	@Test
	public void testDefaultValueBeingAPlaceholder() {
		assertThat(result,
				hasItem(allOf(moduleOptionNamed("post"), moduleOptionWithDefaultValue(equalTo("${xd.stream.name}")))));

	}

}
