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

import java.util.Collection;
import java.util.Map;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.module.config.ChannelExporter;
import org.springframework.integration.module.config.DefaultChannelExporter;
import org.springframework.util.Assert;
import org.springframework.xd.module.SimpleModule;

/**
 * @author David Turanski
 */
public class IntegrationModule extends SimpleModule {

	protected static final String MODULE_TYPE = "integration";

	private volatile MessageChannel inputChannel;

	private Map<String, SubscribableChannel> outputChannels;

	private final ChannelExporter channelExporter;
	private volatile Collection<Resource> additionalComponentLocations;

	/**
	 * @param name
	 */
	public IntegrationModule(String name) {
		super(name, MODULE_TYPE);
		this.channelExporter = new DefaultChannelExporter();
	}
	
	protected IntegrationModule(String name, String type) {
		super(name, type);
		this.channelExporter = new DefaultChannelExporter();
	}

	/**
	 * @param name
	 * @param channelExporter
	 */
	public IntegrationModule(String name, ChannelExporter channelExporter) {
		super(name, MODULE_TYPE);
		Assert.notNull(channelExporter, "ChannelExporter cannot be null");
		this.channelExporter = channelExporter;
	}

	public MessageChannel getInputChannel() {
		return this.inputChannel;
	}

	public Map<String, SubscribableChannel> getOutputChannels() {
		return this.outputChannels;
	}
	
	/**
	 * Set additional component locations
	 * @param additionalComponentLocations
	 */
	public void setAdditionalComponentLocations(Collection<Resource> additionalComponentLocations) {
		this.additionalComponentLocations = additionalComponentLocations;
	}
	
	/**
	 * @return additional component locations
	 */
	public Collection<Resource> getAdditionalComponentLocations() {
		return this.additionalComponentLocations;
	}

	public void initializeChannels() {
		ConfigurableApplicationContext context = (ConfigurableApplicationContext) this.getApplicationContext();
		if (!context.isActive()) {
			Map<String, MessageChannel> messageChannels = context.getBeansOfType(MessageChannel.class);
			this.inputChannel = this.channelExporter.getInputChannel(messageChannels);
			Assert.notNull(inputChannel, "Module '" + this.getName() + "' must contain exactly one input channel");
			this.outputChannels = this.channelExporter.getOutputChannels(messageChannels, SubscribableChannel.class);
		}
	}

	public void activateProfiles(String[] activeProfiles)  {
		if (activeProfiles != null && activeProfiles.length > 0) {
			((AbstractEnvironment) this.getApplicationContext().getEnvironment()).setActiveProfiles(activeProfiles);
		}
	}
}
