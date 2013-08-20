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

import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.ResourceDeployer;

/**
 * Specialization of {@link ResourceDeployer} that deals with taps.
 * 
 * @author David Turanski
 * @author Luke Taylor
 * @author Gunnar Hillert
 */
public class TapDeployer extends AbstractInstancePersistingDeployer<TapDefinition, TapInstance> {

	private final StreamDefinitionRepository streamDefinitionRepository;

	public TapDeployer(TapDefinitionRepository repository, StreamDefinitionRepository streamDefinitionRepository,
			DeploymentMessageSender messageSender, XDParser parser, TapInstanceRepository tapInstanceRepository) {
		super(repository, tapInstanceRepository, messageSender, parser, "tap");
		Assert.notNull(streamDefinitionRepository, "stream definition repository cannot be null");
		this.streamDefinitionRepository = streamDefinitionRepository;
	}

	@Override
	public TapDefinition save(TapDefinition tapDefinition) {
		Assert.notNull(tapDefinition, "tap definition may not be null");
		if (!streamDefinitionRepository.exists(tapDefinition.getStreamName())) {
			throw new NoSuchDefinitionException(tapDefinition.getStreamName(),
					"Can't tap into stream '%s' because it does not exist");
		}
		return super.save(tapDefinition);
	}

	@Override
	protected TapInstance makeInstance(TapDefinition definition) {
		return new TapInstance(definition);
	}
}
