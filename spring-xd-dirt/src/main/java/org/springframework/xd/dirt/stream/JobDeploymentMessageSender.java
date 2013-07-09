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
package org.springframework.xd.dirt.stream;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;

/**
 * @author Glenn Renfro
 *
 */
public class JobDeploymentMessageSender {
 	private final MessageChannel outputChannel;
	private final Map<String, List<ModuleDeploymentRequest>> deployments = new ConcurrentHashMap<String, List<ModuleDeploymentRequest>>();

	public JobDeploymentMessageSender(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}
	public void sendDeploymentRequests(String name, List<ModuleDeploymentRequest> requests) {	 
		this.addDeployment(name, requests);
		for (ModuleDeploymentRequest request : requests) {
			Message<?> message = MessageBuilder.withPayload(request.toString()).build();
			this.outputChannel.send(message);
		};
		
	}
	protected final void addDeployment(String name, List<ModuleDeploymentRequest> modules) {
		this.deployments.put(name, modules);
	}

	protected List<ModuleDeploymentRequest> removeDeployment(String name) {
		return this.deployments.remove(name);
	}


}
