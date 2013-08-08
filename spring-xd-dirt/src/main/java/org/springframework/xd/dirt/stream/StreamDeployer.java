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

import org.springframework.util.Assert;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;

/**
 * Default implementation of {@link StreamDeployer} that emits deployment
 * request messages on a bus and relies on {@link StreamDefinitionRepository}
 * and {@link StreamRepository} for persistence.
 * 
 * @author Mark Fisher
 * @author Gary Russell
 * @author Andy Clement
 * @author Eric Bottard
 * @author Gunnar Hillert
 */
public class StreamDeployer extends AbstractDeployer<StreamDefinition> {

	/**
	 * Stores runtime information about a deployed stream.
	 */
	private final StreamRepository streamRepository;

	public StreamDeployer(StreamDefinitionRepository repository, DeploymentMessageSender messageSender,
			StreamRepository streamRepository, XDParser parser) {
		super(repository, messageSender, parser, "stream");

		Assert.notNull(streamRepository, "streamRepository must not be null");
		this.streamRepository = streamRepository;
	}

	@Override
	public void delete(String name) {
		StreamDefinition def = getRepository().findOne(name);
		if (def == null) {
			throw new NoSuchDefinitionException(name, "Can't delete stream '%s' because it does not exist");
		}
		if (streamRepository.exists(name)) {
			undeploy(name);
		}

		getRepository().delete(name);
	}

	@Override
	public void deploy(String name) {

		Assert.hasText(name, "name cannot be blank or null");

		if (streamRepository.exists(name)) {
			throw new AlreadyDeployedException(name, "The stream named '%s' is already deployed");
		}

		final StreamDefinition definition = getRepository().findOne(name);

		if (definition == null) {
			throwNoSuchDefinitionException(name);
		}

		final List<ModuleDeploymentRequest> requests = parse(name, definition.getDefinition());
		sendDeploymentRequests(name, requests);

		final Stream stream = new Stream(definition);
		streamRepository.save(stream);
	}

	@Override
	public void undeploy(String name) {
		Assert.hasText(name, "name cannot be blank or null");

		final Stream stream = streamRepository.findOne(name);
		if (stream == null) {
			throwNoSuchDefinitionException(name);
		}

		final List<ModuleDeploymentRequest> requests = parse(name, stream.getDefinition().getDefinition());
		Collections.reverse(requests);

		for (ModuleDeploymentRequest request : requests) {
			request.setRemove(true);
		}

		sendDeploymentRequests(name, requests);

		streamRepository.delete(stream);
	}
}
