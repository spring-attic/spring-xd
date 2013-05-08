/*
 * Copyright 2002-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.flow.config.xml;

import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @author David Turanski
 *
 */
public class ImportBeansTest {
	@Test
	public void test() {
		GenericApplicationContext ctx1= new GenericApplicationContext();
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(Foo.class);
		builder.addConstructorArgValue("bar");
		ctx1.registerBeanDefinition("foo", builder.getBeanDefinition());
		ctx1.refresh();
		
		Foo foo = ctx1.getBean(Foo.class);
		
		GenericApplicationContext ctx2= new GenericApplicationContext();
		ctx2.getBeanFactory().registerSingleton("foo", foo);
		ctx2.refresh();
		Foo foo2 = ctx2.getBean(Foo.class);
		assertSame(foo,foo2);
	}
	
	
	static class Foo {
		private String val;
		public Foo(String val) {
			this.val = val;
		}
		public String getVal() {
			return this.val;
		}
	}

}
