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

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;

/**
 * Default implementation of {@link StreamDeployer} that emits deployment request messages on a bus and relies on
 * {@link StreamDefinitionRepository} and {@link StreamRepository} for persistence.
 * 
 * @author Mark Fisher
 * @author Gary Russell
 * @author Andy Clement
 * @author Eric Bottard
 */
public class DefaultStreamDeployer implements StreamDeployer {

	/**
	 * Used to parse the stream definition (DSL) into actionable module deployment requests.
	 */
	private final StreamParser streamParser = new EnhancedStreamParser(); // new DefaultStreamParser();

	/**
	 * The channel into which requests are published.
	 */
	private final MessageChannel outputChannel;

	/**
	 * Stores stream definitions (DSL).
	 */
	private final StreamDefinitionRepository streamDefinitionRepository;

	/**
	 * Stores runtime information about a deployed stream.
	 */
	private final StreamRepository streamRepository;

	public DefaultStreamDeployer(MessageChannel outputChannel, StreamDefinitionRepository streamDefinitionRepository,
			StreamRepository streamRepository) {
		Assert.notNull(outputChannel, "outputChannel must not be null");
		Assert.notNull(streamDefinitionRepository, "streamDefinitionRepository must not be null");
		Assert.notNull(streamRepository, "streamRepository must not be null");
		this.outputChannel = outputChannel;
		this.streamDefinitionRepository = streamDefinitionRepository;
		this.streamRepository = streamRepository;
	}

	@Override
	public StreamDefinition createStream(String name, String config, boolean deploy) {
		if (streamDefinitionRepository.exists(name)) {
			throw new StreamAlreadyExistsException(name);
		}
		// Parse here once for early failure
		this.streamParser.parse(name, config);
		StreamDefinition def = streamDefinitionRepository.save(new StreamDefinition(name, config));
		if (deploy) {
			deployStream(name);
		}
		return def;
	}

	@Override
	public StreamDefinition destroyStream(String name) {
		StreamDefinition def = streamDefinitionRepository.findOne(name);
		if (def == null) {
			throw new NoSuchStreamException(name);
		}
		if (streamRepository.exists(name)) {
			undeployStream(name);
		}
		streamDefinitionRepository.delete(name);
		return def;
	}

	@Override
	public Stream deployStream(String name) {
		if (streamRepository.exists(name)) {
			throw new StreamAlreadyDeployedException(name);
		}
		StreamDefinition def = streamDefinitionRepository.findOne(name);
		if (def == null) {
			throw new NoSuchStreamException(name);
		}
		List<ModuleDeploymentRequest> requests = this.streamParser.parse(name, def.getDefinition());
		for (ModuleDeploymentRequest request : requests) {
			Message<?> message = MessageBuilder.withPayload(request.toString()).build();
			this.outputChannel.send(message);
		}
		Stream stream = new Stream(def);
		streamRepository.save(stream);
		return stream;
	}

	@Override
	public Stream undeployStream(String name) {
		Stream stream = streamRepository.findOne(name);
		if (stream == null) {
			throw new NoSuchStreamException(name);
		}
		List<ModuleDeploymentRequest> modules = streamParser.parse(name, stream.getStreamDefinition().getDefinition());
		// undeploy in the reverse sequence (source first)
		Collections.reverse(modules);
		for (ModuleDeploymentRequest module : modules) {
			module.setRemove(true);
			Message<?> message = MessageBuilder.withPayload(module.toString()).build();
			this.outputChannel.send(message);
		}
		streamRepository.delete(stream);
		return stream;
	}

}
