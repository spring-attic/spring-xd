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
package org.springframework.integration.flow;

import java.util.Map.Entry;

import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.registry.ChannelRegistry;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.flow.interceptor.FlowInterceptor;
import org.springframework.integration.module.IntegrationModule;
import org.springframework.integration.module.IntegrationPlugin;
import org.springframework.xd.module.Module;

/**
 * @author David Turanski
 * @ since 3.0
 *
 */
public class FlowPlugin extends IntegrationPlugin {

	public FlowPlugin(ChannelRegistry channelRegistry) {
		super(channelRegistry);
	}
	
	public void processModule(Module module) {
		this.processModule(module, null, 0);
	}

	public void processModule(Module module, String group, int index) {
		super.processModule(module,group, index);
		IntegrationModule integrationModule = (IntegrationModule) module;
		for (Entry<String, SubscribableChannel> entry: integrationModule.getOutputChannels().entrySet()) {
			AbstractMessageChannel channel = (AbstractMessageChannel) entry.getValue();
			FlowInterceptor interceptor = new FlowInterceptor(entry.getKey());
			channel.addInterceptor(interceptor);
		}
	}
}
