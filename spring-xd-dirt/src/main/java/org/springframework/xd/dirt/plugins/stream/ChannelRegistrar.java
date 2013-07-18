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
package org.springframework.xd.dirt.plugins.stream;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.x.channel.registry.ChannelRegistry;
import org.springframework.util.Assert;
import org.springframework.xd.module.Module;

/**
 * Implementation of {@link BeanPostProcessor} that registers all channels with
 * the name "input" and "output" with the provided {@link ChannelRegistry}. This
 * component also removes those channels from the registry on destroy.
 *
 * @author Jennifer Hickey
 * @author David Turanski
 */
public class ChannelRegistrar implements BeanPostProcessor, DisposableBean {

	private ChannelRegistry channelRegistry;

	private String moduleGroup;

	private int moduleIndex;

	private Module module;

	public ChannelRegistrar(ChannelRegistry channelRegistry, Module module, String moduleGroup, int moduleIndex) {
		Assert.notNull(channelRegistry);
		Assert.notNull(module);
		Assert.hasText(moduleGroup);
		this.channelRegistry = channelRegistry;
		this.module = module;
		this.moduleGroup = moduleGroup;
		this.moduleIndex = moduleIndex;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof MessageChannel) {
			if ("input".equals(beanName)) {
				registerInputChannel((MessageChannel)bean);
			} else if ("output".equals(beanName)) {
				registerOutputChannel((MessageChannel)bean);
			}
		}
		return bean;
	}

	@Override
	public void destroy() throws Exception {
		this.channelRegistry.cleanAll(moduleGroup + "." + moduleIndex);
	}

	private void registerInputChannel(MessageChannel channel) {
		Assert.isTrue(moduleIndex > 0, "a module with an input channel must have an index greater than 0");
		String channelNameInRegistry = moduleGroup + "." + (moduleIndex - 1);
		channelRegistry.inbound(channelNameInRegistry, channel, this.module);
	}

	private void registerOutputChannel(MessageChannel channel) {
		String channelNameInRegistry = moduleGroup + "." + moduleIndex;
		channelRegistry.outbound(channelNameInRegistry, channel, this.module);
	}
}
