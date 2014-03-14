/*
 * Copyright 2011-2014 the original author or authors.
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

import java.util.List;

import org.jboss.netty.channel.ChannelException;

import org.springframework.context.ApplicationContextException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;

/**
 * @author Luke Taylor
 */
public class DeploymentMessageSender {

	private final MessageChannel deployChannel;

	public DeploymentMessageSender(MessageChannel deployChannel) {
		this.deployChannel = deployChannel;
	}

	public void sendDeploymentRequests(String name, List<ModuleDeploymentRequest> requests) {
		for (ModuleDeploymentRequest request : requests) {
			Message<?> message = MessageBuilder.withPayload(request.toString()).build();
			// todo: this is the only use of the sender now; no longer needed once sending via MessageBus
			if (request.isLaunch()) {
				try {
					this.deployChannel.send(message);
				}
				catch (MessageHandlingException e) {
					if (e.getCause() instanceof ApplicationContextException
							&& e.getCause().getCause() instanceof ChannelException) {
						throw new MessageHandlingException(e.getFailedMessage(), e.getCause().getCause().getMessage()
								+ ". Possibly the port is already in use.", e);
					}
					throw e;
				}
			}
		}
	}

}
