/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.module;

import static org.junit.Assert.*;

import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.message.GenericMessage;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.dirt.server.options.OptionUtils;
import org.springframework.xd.dirt.server.options.SingleNodeOptions;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.Module;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.Plugin;
import org.springframework.xd.module.SimpleModule;


/**
 * 
 * @author David Turanski
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ModuleDeployerTests {

	@Autowired
	private ApplicationContext context;

	@BeforeClass
	public static void setUp() {
		OptionUtils.configureRuntime(new SingleNodeOptions(), null);
	}

	@Autowired
	ModuleDeployer moduleDeployer;

	@Test
	public void testModuleContext() {
			ModuleDeploymentRequest request = new ModuleDeploymentRequest();
			request.setGroup("test");
			request.setIndex(0);
			request.setModule("log");
			Message<ModuleDeploymentRequest> message = new GenericMessage<ModuleDeploymentRequest>(request);
			moduleDeployer.handleMessage(message);
	}

	public static class TestPlugin implements Plugin {
		private ApplicationContext moduleCommonContext;
		@Override
		public void preProcessModule(Module module) {
			assertEquals("module commonContext should not contain any Plugins",0, moduleCommonContext.getBeansOfType(Plugin.class).size());
			moduleCommonContext.getBean("mbeanServer");
		}

		@Override
		public void postProcessModule(Module module) {
		}

		@Override
		public void removeModule(Module module) {

		}

		@Override
		public void postProcessSharedContext(ConfigurableApplicationContext moduleCommonContext) {
			this.moduleCommonContext = moduleCommonContext;
			assertTrue(moduleCommonContext.getEnvironment().acceptsProfiles("!xd.jmx.enabled"));
			moduleCommonContext.getEnvironment().addActiveProfile("xd.jmx.enabled");
		}

	}

}
