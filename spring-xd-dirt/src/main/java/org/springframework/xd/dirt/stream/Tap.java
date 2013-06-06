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

package org.springframework.xd.dirt.stream;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.x.channel.registry.ChannelRegistry;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public class Tap implements InitializingBean {

	private final String tapPoint;

	private volatile MessageChannel outputChannel;

	private final ChannelRegistry channelRegistry;

	public Tap(String tapPoint, ChannelRegistry channelRegistry) {
		Assert.hasText(tapPoint, "tapPoint must not be empty or null");
		Assert.notNull(channelRegistry, "channelRegistry must not be null");
		this.tapPoint = (tapPoint.matches("^.*\\.\\d+$") ? tapPoint : tapPoint + ".0");
		this.channelRegistry = channelRegistry;
	}

	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.outputChannel, "outputChannel must not be null");
		this.channelRegistry.tap(this.tapPoint, this.outputChannel);
	}

}
