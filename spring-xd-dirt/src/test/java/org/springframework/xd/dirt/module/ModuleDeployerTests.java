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

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.Plugin;
import org.springframework.xd.module.options.DefaultModuleOptionsMetadataResolver;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;


/**
 * 
 * @author David Turanski
 */
public class ModuleDeployerTests {

	private ModuleDeployer moduleDeployer;

	private ConfigurableApplicationContext context;

	@EnableAutoConfiguration
	@Configuration
	@ImportResource({
		"META-INF/spring-xd/internal/container.xml",
		"META-INF/spring-xd/store/${XD_STORE}-store.xml" })
	protected static class ModuleDeployerTestsConfiguration {

		@Bean
		public ModuleOptionsMetadataResolver moduleOptionsMetadataResolver() {
			return new DefaultModuleOptionsMetadataResolver();
		}
	}

	@Before
	public void setUp() {
		context = new SpringApplicationBuilder(ModuleDeployerTestsConfiguration.class, TestPlugin.class).profiles(
				"node", "memory").web(false).run("--XD_STORE=memory");
		moduleDeployer = context.getBean(ModuleDeployer.class);
	}

	@After
	public void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void testModuleContext() {
		ModuleDeploymentRequest request = new ModuleDeploymentRequest();
		request.setGroup("test");
		request.setIndex(0);
		request.setType(ModuleType.sink);
		request.setModule("log");
		Message<ModuleDeploymentRequest> message = new GenericMessage<ModuleDeploymentRequest>(request);
		moduleDeployer.handleMessage(message);
	}

	public static class TestPlugin implements Plugin {

		private ApplicationContext moduleCommonContext;

		@Override
		public void preProcessModule(Module module) {
			assertEquals("module commonContext should not contain any Plugins", 0,
					moduleCommonContext.getBeansOfType(Plugin.class).size());
			Properties properties = new Properties();
			properties.setProperty("xd.stream.name", module.getDeploymentMetadata().getGroup());
			module.addProperties(properties);
		}

		@Override
		public void postProcessModule(Module module) {
		}

		@Override
		public void beforeShutdown(Module module) {
		}

		@Override
		public void removeModule(Module module) {

		}

		@Override
		public boolean supports(Module module) {
			return true;
		}

		@Override
		public void preProcessSharedContext(ConfigurableApplicationContext moduleCommonContext) {
			this.moduleCommonContext = moduleCommonContext;
		}
	}

}
