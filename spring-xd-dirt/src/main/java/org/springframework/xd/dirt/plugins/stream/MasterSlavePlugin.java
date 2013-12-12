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

package org.springframework.xd.dirt.plugins.stream;

import org.springframework.integration.x.bus.MessageBus;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.Module;


/**
 * A plugin for modules that (perhaps in addition to normal 'input' and 'output' channels) has two pairs a request/reply
 * channels where one instance can farm out work to other instances and collect responses.
 * 
 * @author Gary Russell
 */
public class MasterSlavePlugin extends StreamPlugin {

	@Override
	public void postProcessModule(Module module) {
		super.postProcessModule(module); // may have normal input and output channels
		MessageBus bus = findMessageBus(module);
		bindRequestor(module, bus);
		bindResponder(module, bus);
	}

	private void bindRequestor(Module module, MessageBus bus) {
		DeploymentMetadata md = module.getDeploymentMetadata();
		MessageChannel requests = module.getComponent("requests.out", MessageChannel.class);
		if (requests != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("binding request/response channels [requests.out, responses.in] for " + module);
			}
			MessageChannel responses = module.getComponent("responses.in", MessageChannel.class);
			Assert.notNull(responses);
			String name = this.requestorName(md);
			bus.bindRequestor(name, requests, responses);
		}
	}

	private void bindResponder(Module module, MessageBus bus) {
		DeploymentMetadata md = module.getDeploymentMetadata();
		MessageChannel requests = module.getComponent("requests.in", MessageChannel.class);
		if (requests != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("binding request/response channels [requests.in, responses.out] for " + module);
			}
			MessageChannel responses = module.getComponent("responses.out", MessageChannel.class);
			Assert.notNull(responses);
			String name = this.responderName(md);
			// TODO: add the capability for a responder to be bound to a different requestor
			String requestorName = this.requestorName(md);
			bus.bindResponder(name, requestorName, requests, responses);
		}
	}

	private String requestorName(DeploymentMetadata md) {
		return md.getGroup() + "." + md.getIndex() + ".requestor";
	}

	private String responderName(DeploymentMetadata md) {
		return md.getGroup() + "." + md.getIndex() + ".responder";
	}

	@Override
	public void beforeShutdown(Module module) {
		super.beforeShutdown(module);
		DeploymentMetadata md = module.getDeploymentMetadata();
		MessageBus bus = findMessageBus(module);
		if (bus != null) {
			MessageChannel channel = module.getComponent("requests.out", MessageChannel.class);
			if (channel != null) {
				bus.unbindProducer(this.requestorName(md), channel);
			}
			channel = module.getComponent("responses.in", MessageChannel.class);
			if (channel != null) {
				bus.unbindConsumer(this.requestorName(md), channel);
			}
			channel = module.getComponent("responses.out", MessageChannel.class);
			if (channel != null) {
				bus.unbindProducer(this.requestorName(md), channel);
			}
			channel = module.getComponent("requests.in", MessageChannel.class);
			if (channel != null) {
				bus.unbindConsumer(this.requestorName(md), channel);
			}
		}
	}

}
