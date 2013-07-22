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

package org.springframework.xd.dirt.stream;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.data.repository.CrudRepository;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.BaseDefinition;
import org.springframework.xd.dirt.core.ResourceDeployer;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;

/**
 * Abstract implementation of the @link {@link org.springframework.xd.dirt.core.ResourceDeployer} interface. It provides the basic support for calling
 * CrudRepository methods and sending deployment messages.
 * 
 * @author Luke Taylor
 * @author Mark Pollack
 * @author Eric Bottard
 */
public abstract class AbstractDeployer<D extends BaseDefinition> implements ResourceDeployer<D> {
	private CrudRepository<D, String> repository;

	private final StreamParser streamParser = new EnhancedStreamParser();

	private final DeploymentMessageSender messageSender;

	protected final String NAME_EMPTY_ERROR = "name cannot be blank or null";
	protected final String REPOSITORY_NAME_NULL_ERROR = "repository cannot be null";
	protected final String MESSAGE_SENDER_NAME_NULL_ERROR = "message sender cannot be null";
	protected final String DEFINITION_KIND_EMPTY_ERROR = "definitionKind cannot be blank";
	protected final String DEFINITION_NULL_ERROR = "Definition cannot be null";
	
	protected final String DEFINITION_ERROR_PREFIX = "There is already a ";
	protected final String DEFINITION_ERROR_SUFFIX = " named '%s'";
	protected final String NO_DEFINITION_PREFIX = "There is no ";
	protected final String NO_DEFINITION_SUFFIX = " definition named '%s'";
	protected final String ALREADY_DEPLOYED_EXCEPTION_PREFIX = "The ";
	protected final String ALREADY_DEPLOYED_EXCEPTION_SUFFIX = " named '%s' is already deployed";
	
	/**
	 * Lower-case, singular name of the kind of definition we're deploying. Used in exception messages.
	 */
	private final String definitionKind;

	protected AbstractDeployer(CrudRepository<D, String> repository, DeploymentMessageSender messageSender,
			String definitionKind) {
		Assert.notNull(repository, REPOSITORY_NAME_NULL_ERROR);
		Assert.notNull(messageSender, MESSAGE_SENDER_NAME_NULL_ERROR);
		Assert.hasText(definitionKind, DEFINITION_KIND_EMPTY_ERROR);
		this.repository = repository;
		this.messageSender = messageSender;
		this.definitionKind = definitionKind;
	}

	public D save(D definition) {
		Assert.notNull(definition, "Definition may not be null");
		if (repository.findOne(definition.getName()) != null) {
			throwDefinitionAlreadyExistsException(definition);
		}
		return repository.save(definition);
	}

	protected void throwDefinitionAlreadyExistsException(D definition) {
		throw new DefinitionAlreadyExistsException(definition.getName(), DEFINITION_ERROR_PREFIX + definitionKind
				+ DEFINITION_ERROR_SUFFIX);
	}

	protected void throwNoSuchDefinitionException(String name) {
		throw new NoSuchDefinitionException(name, NO_DEFINITION_PREFIX
				+ definitionKind + NO_DEFINITION_SUFFIX);
	}

	protected void throwAlreadyDeployedException(String name) {
		throw new AlreadyDeployedException(name,
				ALREADY_DEPLOYED_EXCEPTION_PREFIX + definitionKind
						+ ALREADY_DEPLOYED_EXCEPTION_SUFFIX);
	}

	@Override
	public void deploy(String name) {
		Assert.hasText(name, NAME_EMPTY_ERROR);

		D definition = repository.findOne(name);

		if (definition == null) {
			throwNoSuchDefinitionException(name);
		}
		List<ModuleDeploymentRequest> requests = streamParser.parse(name, definition.getDefinition());
		messageSender.sendDeploymentRequests(name, requests);
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

	protected void sendDeploymentRequests(String name,
			List<ModuleDeploymentRequest> requests) {
		messageSender.sendDeploymentRequests(name, requests);
	}
	
	protected List<ModuleDeploymentRequest> parse(String name, String config) {
		return streamParser.parse(name, config);
	}


}
