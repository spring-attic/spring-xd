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

import java.util.Collections;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;

/**
 * @author David Turanski
 * @author Luke Taylor
 * @author Gunnar Hillert
 */
public class TapDeployer extends AbstractDeployer<TapDefinition> {

	private final StreamDefinitionRepository streamRepository;

	/**
	 * Stores runtime information about a deployed tap.
	 */
	private final TapInstanceRepository tapInstanceRepository;

	public TapDeployer(TapDefinitionRepository repository, StreamDefinitionRepository streamDefinitionRepository,
			DeploymentMessageSender messageSender, TapInstanceRepository tapInstanceRepository) {
		super(repository, messageSender, "tap");
		Assert.notNull(streamDefinitionRepository, "stream definition repository cannot be null");
		this.streamRepository = streamDefinitionRepository;
		this.tapInstanceRepository = tapInstanceRepository;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.xd.dirt.core.ResourceDeployer#create(java.lang.Object)
	 */
	@Override
	public TapDefinition save(TapDefinition tapDefinition) {
		Assert.notNull(tapDefinition, "tap definition may not be null");
		if (!streamRepository.exists(tapDefinition.getStreamName())) {
			throw new NoSuchDefinitionException(tapDefinition.getStreamName(),
					"Can't tap into stream '%s' because it does not exist");
		}
		return super.save(tapDefinition);
	}

	@Override
	public void delete(String name) {
		final TapDefinition tapDefinition = getRepository().findOne(name);
		if (tapDefinition == null) {
			throw new NoSuchDefinitionException(name,
				"Can't delete tap '%s' because it does not exist");
		}
		if (tapInstanceRepository.exists(name)) {
			// Undeploy the tap before delete it from repository
			undeploy(name);
		}

		getRepository().delete(name);
	}

	@Override
	public void deploy(String name) {

		Assert.hasText(name, "name cannot be blank or null");

		if (tapInstanceRepository.exists(name)) {
			throw new AlreadyDeployedException(name, "The tap named '%s' is already deployed");
		}

		final TapDefinition tapDefinition = getRepository().findOne(name);

		if (tapDefinition == null) {
			throwNoSuchDefinitionException(name);
		}

		final List<ModuleDeploymentRequest> requests = parse(name,
				tapDefinition.getDefinition());
		sendDeploymentRequests(name, requests);

		final TapInstance tapInstance = new TapInstance(tapDefinition);
		tapInstanceRepository.save(tapInstance);
	}

	public void undeploy(String name) {
		Assert.hasText(name, "name cannot be blank or null");

		final TapInstance tapInstance = tapInstanceRepository.findOne(name);
		if (tapInstance == null) {
			throwNoSuchDefinitionException(name);
		}

		final List<ModuleDeploymentRequest> requests = parse(name, tapInstance
				.getDefinition().getDefinition());

		Collections.reverse(requests);

		for (ModuleDeploymentRequest request : requests) {
			request.setRemove(true);
		}

		sendDeploymentRequests(name, requests);

		tapInstanceRepository.delete(tapInstance);
	}

}
