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
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.ResourceDeployer;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;

/**
 * @author David Turanski
 *
 */
public class TapDeployer implements ResourceDeployer<TapDefinition> {
	private final TapDefinitionRepository repository;
	private final TapDeploymentMessageSender messageSender;
	private final StreamDefinitionRepository streamRepository;
	private final StreamParser streamParser = new EnhancedStreamParser();

	public TapDeployer(TapDefinitionRepository repository, StreamDefinitionRepository streamRepository, TapDeploymentMessageSender messageSender) {
		Assert.notNull(repository, "repository cannot be null");
		Assert.notNull(streamRepository, "stream repository cannot be null");
		Assert.notNull(messageSender, "message sender cannot be null");
		this.repository = repository;
		this.messageSender = messageSender;
		this.streamRepository = streamRepository;

	}

	/* (non-Javadoc)
	 * @see org.springframework.xd.dirt.core.ResourceDeployer#deploy(java.lang.String)
	 */
	@Override
	public void deploy(String name) {
		Assert.hasText(name, "name cannot be blank or null");

		TapDefinition tapDefinition = repository.findOne(name);
		Assert.notNull(tapDefinition, "tap '" + name+ " not found");
		List<ModuleDeploymentRequest> requests = streamParser.parse(name, tapDefinition.getDefinition());
		messageSender.sendDeploymentRequests(name, requests);
	}

	/* (non-Javadoc)
	 * @see org.springframework.xd.dirt.core.ResourceDeployer#create(java.lang.Object)
	 */
	@Override
	public TapDefinition create(TapDefinition tapDefinition) {
		Assert.notNull(tapDefinition, "tap definition may not be null");
		Assert.isTrue(streamRepository.exists(tapDefinition.getStreamName()),
				"source stream '" + tapDefinition.getStreamName()+"' does not exist for tap '" + tapDefinition.getName());
		return repository.save(tapDefinition);
	}

	@Override
	public Iterable<TapDefinition> findAll() {
		final SortedSet<TapDefinition> sortedTapDefinitions = new TreeSet<TapDefinition>();
		for (TapDefinition tapDefinition : repository.findAll()) {
			sortedTapDefinitions.add(tapDefinition);
		}
		return sortedTapDefinitions;
	}

	@Override
	public TapDefinition findOne(String name) {
		return repository.findOne(name);
	}
}
