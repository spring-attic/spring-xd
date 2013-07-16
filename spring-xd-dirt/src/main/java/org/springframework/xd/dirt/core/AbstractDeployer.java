/*
 * Copyright 2011-2013 the original author or authors.
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

package org.springframework.xd.dirt.core;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.data.repository.CrudRepository;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.stream.*;

/**
 * @author Luke Taylor
 */
public abstract class AbstractDeployer<R extends BaseDefinition> implements ResourceDeployer<R> {
	private CrudRepository<R,String> repository;
	private final StreamParser streamParser = new EnhancedStreamParser();
	private final DeploymentMessageSender messageSender;

	protected AbstractDeployer(CrudRepository<R, String> repository, DeploymentMessageSender messageSender) {
		Assert.notNull(repository, "repository cannot be null");
		Assert.notNull(messageSender, "message sender cannot be null");
		this.repository = repository;
		this.messageSender = messageSender;
	}

	@Override
	public R create(R resourceDefinition) {
		Assert.notNull(resourceDefinition, "Resource definition may not be null");
		return repository.save(resourceDefinition);
	}

	@Override
	public void deploy(String name) {
		Assert.hasText(name, "name cannot be blank or null");

		R definition = repository.findOne(name);

		if (definition == null) {
			throw new NoSuchResourceDefinitionException("Resource definition " + name + " not found.");
		}
		List<ModuleDeploymentRequest> requests = streamParser.parse(name, definition.getDefinition());
		messageSender.sendDeploymentRequests(name, requests);
	}

	@Override
	public R findOne(String name) {
		return repository.findOne(name);
	}

	@Override
	public Iterable<R> findAll() {
		final SortedSet<R> definitions = new TreeSet<R>();
		for (R definition : repository.findAll()) {
			definitions.add(definition);
		}
		return definitions;
	}
}
