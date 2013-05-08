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

package org.springframework.integration.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.channel.registry.ChannelRegistry;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.util.Assert;
import org.springframework.xd.module.Module;
import org.springframework.xd.module.Plugin;

/**
 * @author David Turanski
 * @since 3.0
 */
public class IntegrationPlugin implements Plugin {

	private volatile String integrationModuleBasePath = "/META-INF/spring/integration/module";

	private final ChannelRegistry channelRegistry;

	public IntegrationPlugin(ChannelRegistry channelRegistry) {
		this.channelRegistry = channelRegistry;
	}

	/**
	 * @return the integrationModuleBasePath
	 */
	public String getIntegrationModuleBasePath() {
		return integrationModuleBasePath;
	}

	/**
	 * @param integrationModuleBasePath the integrationModuleBasePath to set
	 */
	void setIntegrationModuleBasePath(String integrationModuleBasePath) {
		Assert.hasText(integrationModuleBasePath, "'integrationModuleBasePath' cannot be empty or null");
		this.integrationModuleBasePath = integrationModuleBasePath;
	}

	/* (non-Javadoc)
	 * @see org.springframework.xd.module.Plugin#processModule(org.springframework.xd.module.Module)
	 */
	@Override
	public void processModule(Module module, String group, int index) {
		if (module.isRunning()) {
			return;
		}
		Assert.notNull(module, "module cannot be null");
		Assert.isAssignable(IntegrationModule.class, module.getClass());	
		String resourcePath = this.integrationModuleBasePath + "/" + module.getName() + ".xml";
		IntegrationModule integrationModule = (IntegrationModule) module;
		List<Resource> resources = new ArrayList<Resource>();
		resources.add(new ClassPathResource(resourcePath));
		if (integrationModule.getAdditionalComponentLocations() != null) {
			resources.addAll(integrationModule.getAdditionalComponentLocations());
		}
		integrationModule.addComponents(resources);
		integrationModule.initializeChannels();
		integrationModule.start();
		channelRegistry.inbound(integrationModule.getInstanceId()+".input",integrationModule.getInputChannel());
		for (Entry<String, SubscribableChannel> entry: integrationModule.getOutputChannels().entrySet()) {
			channelRegistry.outbound(integrationModule.getInstanceId() + ".output", entry.getValue());
		}
	}

}
