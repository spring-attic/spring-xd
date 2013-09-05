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

package org.springframework.integration.x.channel.registry;

import org.springframework.context.Lifecycle;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;


/**
 * Represents a Bridge between a specified channel and an endpoint. The bridge could be in either direction.
 * 
 * @author Jennifer Hickey
 */
public class Bridge implements Lifecycle {

	private MessageChannel channel;

	private AbstractEndpoint endpoint;

	public Bridge(MessageChannel channel, AbstractEndpoint endpoint) {
		this.channel = channel;
		this.endpoint = endpoint;
	}

	public MessageChannel getChannel() {
		return channel;
	}

	public AbstractEndpoint getEndpoint() {
		return endpoint;
	}

	@Override
	public void start() {
		endpoint.start();
	}

	@Override
	public void stop() {
		endpoint.stop();
	}

	@Override
	public boolean isRunning() {
		return endpoint.isRunning();
	}

	@Override
	public String toString() {
		return "Bridge[channel=" + channel + ", endpoint=" + endpoint.getComponentName() + "]";
	}
}
