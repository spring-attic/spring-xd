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

package org.springframework.xd.dirt.plugins;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.x.bus.AbstractTestMessageBus;
import org.springframework.xd.dirt.plugins.stream.StreamPlugin;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.Plugin;


/**
 * Test support class for {@link StreamPlugin}.
 * 
 * @author Ilayaperumal Gopinathan
 */
public class TestStreamPlugin implements Plugin {

	StreamPlugin streamPlugin;

	AbstractTestMessageBus testMessageBus;

	public TestStreamPlugin(StreamPlugin streamPlugin, AbstractTestMessageBus testMessageBus) {
		this.streamPlugin = streamPlugin;
		this.testMessageBus = testMessageBus;
	}

	@Override
	public void preProcessModule(Module module) {
		this.streamPlugin.preProcessModule(module);
	}

	@Override
	public void postProcessModule(Module module) {
		this.streamPlugin.postProcessModule(module);
	}

	@Override
	public void removeModule(Module module) {
		this.streamPlugin.removeModule(module);
	}

	@Override
	public void beforeShutdown(Module module) {
		this.streamPlugin.beforeShutdown(module);
	}

	@Override
	public void preProcessSharedContext(ConfigurableApplicationContext context) {
		context.getBeanFactory().registerSingleton("messageBus", testMessageBus);
	}

	@Override
	public boolean supports(Module module) {
		return this.streamPlugin.supports(module);
	}


}
