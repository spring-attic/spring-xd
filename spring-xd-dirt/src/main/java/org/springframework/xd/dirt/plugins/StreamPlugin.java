/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.plugins;

import java.util.Map;
import java.util.Properties;

import org.springframework.integration.MessageChannel;
import org.springframework.integration.x.channel.registry.ChannelRegistry;
import org.springframework.util.Assert;
import org.springframework.xd.module.Module;
import org.springframework.xd.module.Plugin;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class StreamPlugin implements Plugin {

	private final ChannelRegistry channelRegistry;

	public StreamPlugin(ChannelRegistry channelRegistry) {
		Assert.notNull(channelRegistry, "channelRegistry must not be null");
		this.channelRegistry = channelRegistry;
	}

	@Override
	public void processModule(Module module, String group, int index) {
		String type = module.getType();
		if (("source".equals(type) || "processor".equals(type) || "sink".equals(type)) && group != null) {
			this.registerChannels(module.getComponents(MessageChannel.class), group, index);
			this.configureProperties(module, group);
		}
	}

	@Override
	public void removeModule(Module module, String group, int index) {
		this.channelRegistry.cleanAll(group + "."  + index);
	}

	private void registerChannels(Map<String, MessageChannel> channels, String group, int index) {
		for (Map.Entry<String, MessageChannel> entry : channels.entrySet()) {
			if ("input".equals(entry.getKey())) {
				Assert.isTrue(index > 0, "a module with an input channel must have an index greater than 0");
				String channelNameInRegistry = group + "." + (index - 1);
				channelRegistry.inbound(channelNameInRegistry, entry.getValue());
			}
			else if ("output".equals(entry.getKey())) {
				String channelNameInRegistry = group + "." + index;
				channelRegistry.outbound(channelNameInRegistry, entry.getValue());
			}
		}
	}

	private void configureProperties(Module module, String group) {
		Properties properties = new Properties();
		properties.setProperty("xd.stream.name", group);
		module.addProperties(properties);
	}

}
