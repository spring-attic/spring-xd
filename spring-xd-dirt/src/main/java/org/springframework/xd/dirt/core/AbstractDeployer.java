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
import org.springframework.xd.dirt.stream.BaseDefinition;
import org.springframework.xd.dirt.stream.DefinitionAlreadyExistsException;
import org.springframework.xd.dirt.stream.DeploymentMessageSender;
import org.springframework.xd.dirt.stream.EnhancedStreamParser;
import org.springframework.xd.dirt.stream.NoSuchDefinitionException;
import org.springframework.xd.dirt.stream.StreamParser;

/**
 * Abstract implementation of the @link {@link ResourceDeployer} interface. It provides the basic support for calling
 * CrudRepository methods and sending deployment messages.
 * 
 * @author Luke Taylor
 * @author Mark Pollack
 */
public abstract class AbstractDeployer<D extends BaseDefinition> implements ResourceDeployer<D> {
	private CrudRepository<D, String> repository;

	private final StreamParser streamParser = new EnhancedStreamParser();

	private final DeploymentMessageSender messageSender;

	protected AbstractDeployer(CrudRepository<D, String> repository, DeploymentMessageSender messageSender) {
		Assert.notNull(repository, "repository cannot be null");
		Assert.notNull(messageSender, "message sender cannot be null");
		this.repository = repository;
		this.messageSender = messageSender;
	}

	@Override
	public D create(D definition) {
		Assert.notNull(definition, "Definition may not be null");
		if (repository.findOne(definition.getName()) != null) {
			throw createDefinitionAlreadyExistsException(definition);
		}
		return repository.save(definition);
	}

	/**
	 * Intented to be overridden by subclasses that want to throw a more specific exception when a Definition already
	 * exists, e.g. JobAlreadyExistsException.
	 * @param resourceDefinition the class taht defines the
	 * @return
	 */
	protected XDRuntimeException createDefinitionAlreadyExistsException(D definition) {
		return new DefinitionAlreadyExistsException(definition.getName(), "Definition " + definition.getName()
				+ " already exists");
	}

	@Override
	public void deploy(String name) {
		Assert.hasText(name, "name cannot be blank or null");

		D definition = repository.findOne(name);

		if (definition == null) {
			throw createNoSuchDefinitionException(name);
		}
		List<ModuleDeploymentRequest> requests = streamParser.parse(name, definition.getDefinition());
		messageSender.sendDeploymentRequests(name, requests);
	}

	protected XDRuntimeException createNoSuchDefinitionException(String name) {
		return new NoSuchDefinitionException(name, "Resource definition " + name + " not found.");
	}

	@Override
	public D findOne(String name) {
		return repository.findOne(name);
	}

	@Override
	public Iterable<D> findAll() {
		final SortedSet<D> definitions = new TreeSet<D>();
		for (D definition : repository.findAll()) {
			definitions.add(definition);
		}
		return definitions;
	}

	protected CrudRepository<D, String> getRepository() {
		return repository;
	}

	public DeploymentMessageSender getMessageSender() {
		return messageSender;
	}
}
