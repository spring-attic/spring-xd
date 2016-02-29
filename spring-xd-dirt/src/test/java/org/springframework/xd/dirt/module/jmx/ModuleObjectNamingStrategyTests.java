/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.xd.dirt.module.jmx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Properties;

import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jmx.support.JmxUtils;


/**
 *
 * @author Gary Russell
 */
public class ModuleObjectNamingStrategyTests {

	@Test
	public void testSimpleBeanName() throws Exception {
		Properties props = new Properties();
		props.setProperty("group", "fooGroup");
		props.setProperty("label", "fooLabel");
		props.setProperty("type", "fooType");
		props.setProperty("sequence", "fooSequence");
		ModuleObjectNamingStrategy namer = new ModuleObjectNamingStrategy("foo", props);
		Object managedBean = new Object();
		assertEquals(new ObjectName("foo:module=fooGroup.fooType.fooLabel.fooSequence,component=Object,name=foo"),
				namer.getObjectName(managedBean, "foo"));
		managedBean = new Foo();
		assertEquals(new ObjectName("foo:module=fooGroup.fooType.fooLabel.fooSequence,"
						+ "component=ModuleObjectNamingStrategyTests.Foo,name=foo"),
				namer.getObjectName(managedBean, "foo"));
	}

	@Test
	public void testXmlContext() throws Exception {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"META-INF/spring-xd/plugins/jmx/mbean-exporters.xml",
				"org/springframework/xd/dirt/module/jmx/test.xml");
		MBeanServer server = JmxUtils.locateMBeanServer();
		MBeanInfo mBeanInfo = server.getMBeanInfo(new ObjectName(
				"xd.fooStream:module=fooGroup.fooType.fooModule.0,"
				+ "component=MessageHistoryConfigurer,name=messageHistoryConfigurer"));
		assertNotNull(mBeanInfo);
		ctx.close();
	}

	public static class Foo {

	}

}
