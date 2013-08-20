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

package org.springframework.xd.dirt.container;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.dirt.server.options.AbstractOptions;
import org.springframework.xd.dirt.server.options.Transport;
import org.springframework.xd.dirt.server.options.XdPropertyKeys;

/**
 * @author David Turanski
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class PropertiesConfigurationTests {

	@Autowired
	ApplicationContext ctx;

	@Test
	public void testSingletonPPC() {
		assertEquals(1, ctx.getBeansOfType(PropertySourcesPlaceholderConfigurer.class).size());
	}

	@BeforeClass
	public static void setup() {
		System.setProperty(XdPropertyKeys.XD_TRANSPORT,"redis");
		System.setProperty(XdPropertyKeys.XD_HOME, new File("..").getAbsolutePath());
	}

	@AfterClass
	public static void tearDown() {
		System.clearProperty("xd.transport");
		System.clearProperty("xd.home");
	}
}
