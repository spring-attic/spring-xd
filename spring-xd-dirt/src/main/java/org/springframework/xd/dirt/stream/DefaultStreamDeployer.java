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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;

/**
 * Default implementation of {@link StreamDeployer} that emits deployment request messages on a bus and relies on a
 * {@link StreamDefinitionRepository} for persistence.
 * 
 * @author Mark Fisher
 * @author Gary Russell
 * @author Andy Clement
 * @author Eric Bottard
 */
public class DefaultStreamDeployer implements StreamDeployer {

	private final StreamParser streamParser = new EnhancedStreamParser(); // new DefaultStreamParser();

	private final MessageChannel outputChannel;

	private final Map<String, List<ModuleDeploymentRequest>> deployments = new ConcurrentHashMap<String, List<ModuleDeploymentRequest>>();

	private final StreamDefinitionRepository streamDefinitionRepository;

	public DefaultStreamDeployer(MessageChannel outputChannel, StreamDefinitionRepository streamDefinitionRepository) {
		Assert.notNull(outputChannel, "outputChannel must not be null");
		this.outputChannel = outputChannel;
		this.streamDefinitionRepository = streamDefinitionRepository;
	}

	@Override
	public StreamDefinition deployStream(String name, String config) {
		if (streamDefinitionRepository.exists(name)) {
			throw new StreamAlreadyExistsException(name);
		}
		List<ModuleDeploymentRequest> requests = this.streamParser.parse(name, config);
		StreamDefinition def = streamDefinitionRepository.save(new StreamDefinition(name, config));
		this.addDeployment(name, requests);
		for (ModuleDeploymentRequest request : requests) {
			Message<?> message = MessageBuilder.withPayload(request.toString()).build();
			this.outputChannel.send(message);
		}
		return def;
	}

	@Override
	public StreamDefinition undeployStream(String name) {
		StreamDefinition def = streamDefinitionRepository.findOne(name);
		if (def == null) {
			throw new NoSuchStreamException(name);
		}
		streamDefinitionRepository.delete(name);
		List<ModuleDeploymentRequest> modules = this.removeDeployment(name);
		if (modules != null) {
			// undeploy in the reverse sequence (source first)
			Collections.reverse(modules);
			for (ModuleDeploymentRequest module : modules) {
				module.setRemove(true);
				Message<?> message = MessageBuilder.withPayload(module.toString()).build();
				this.outputChannel.send(message);
			}
		}
		return def;
	}

	protected final void addDeployment(String name, List<ModuleDeploymentRequest> modules) {
		this.deployments.put(name, modules);
	}

	protected List<ModuleDeploymentRequest> removeDeployment(String name) {
		return this.deployments.remove(name);
	}

}
