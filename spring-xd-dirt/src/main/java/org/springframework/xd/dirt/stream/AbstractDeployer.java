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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.BaseDefinition;
import org.springframework.xd.dirt.core.ResourceDeployer;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.module.ModuleDefinition;

/**
 * Abstract implementation of the @link {@link org.springframework.xd.dirt.core.ResourceDeployer} interface. It provides
 * the basic support for calling CrudRepository methods and sending deployment messages.
 * 
 * @author Luke Taylor
 * @author Mark Pollack
 * @author Eric Bottard
 * @author Andy Clement
 */
public abstract class AbstractDeployer<D extends BaseDefinition> implements ResourceDeployer<D> {

	private PagingAndSortingRepository<D, String> repository;

	protected final XDParser streamParser;

	private final DeploymentMessageSender messageSender;

	/**
	 * Lower-case, singular name of the kind of definition we're deploying. Used in exception messages.
	 */
	protected final String definitionKind;

	protected AbstractDeployer(PagingAndSortingRepository<D, String> repository, DeploymentMessageSender messageSender,
			XDParser parser, String definitionKind) {
		Assert.notNull(repository, "Repository cannot be null");
		Assert.notNull(messageSender, "Message sender cannot be null");
		Assert.hasText(definitionKind, "Definition kind cannot be blank");
		this.repository = repository;
		this.messageSender = messageSender;
		this.definitionKind = definitionKind;
		this.streamParser = parser;
	}

	@Override
	public D save(D definition) {
		Assert.notNull(definition, "Definition may not be null");
		if (repository.findOne(definition.getName()) != null) {
			throwDefinitionAlreadyExistsException(definition);
		}
		List<ModuleDeploymentRequest> moduleDeploymentRequests = streamParser.parse(definition.getName(),
				definition.getDefinition());
		List<ModuleDefinition> moduleDefinitions = createModuleDefinitions(moduleDeploymentRequests);
		if (!moduleDefinitions.isEmpty()) {
			definition.setModuleDefinitions(moduleDefinitions);
		}
		D savedDefinition = repository.save(definition);
		return afterSave(savedDefinition);
	}

	/**
	 * Create a list of ModuleDefinitions given the results of parsing the definition.
	 * 
	 * @param moduleDeploymentRequests The list of ModuleDeploymentRequest resulting from parsing the definition.
	 * @return a list of ModuleDefinitions
	 */
	private List<ModuleDefinition> createModuleDefinitions(List<ModuleDeploymentRequest> moduleDeploymentRequests) {
		List<ModuleDefinition> moduleDefinitions = new ArrayList<ModuleDefinition>(moduleDeploymentRequests.size());

		for (ModuleDeploymentRequest moduleDeploymentRequest : moduleDeploymentRequests) {
			ModuleDefinition moduleDefinition = new ModuleDefinition(moduleDeploymentRequest.getModule(),
					moduleDeploymentRequest.getType());
			moduleDefinitions.add(moduleDefinition);
		}

		return moduleDefinitions;
	}

	/**
	 * Callback method that subclasses may override to get a chance to act on newly saved definitions.
	 */
	protected D afterSave(D savedDefinition) {
		return savedDefinition;
	}

	protected void throwDefinitionAlreadyExistsException(D definition) {
		throw new DefinitionAlreadyExistsException(definition.getName(), String.format(
				"There is already a %s named '%%s'", definitionKind));
	}

	protected void throwNoSuchDefinitionException(String name) {
		throw new NoSuchDefinitionException(name,
				String.format("There is no %s definition named '%%s'", definitionKind));
	}

	protected void throwDefinitionNotDeployable(String name) {
		throw new NoSuchDefinitionException(name,
				String.format("The %s named '%%s' cannot be deployed", definitionKind));
	}

	protected void throwNoSuchDefinitionException(String name, String definitionKind) {
		throw new NoSuchDefinitionException(name,
				String.format("There is no %s definition named '%%s'", definitionKind));
	}

	protected void throwNotDeployedException(String name) {
		throw new NotDeployedException(name, String.format("The %s named '%%s' is not currently deployed",
				definitionKind));
	}

	protected void throwAlreadyDeployedException(String name) {
		throw new AlreadyDeployedException(name,
				String.format("The %s named '%%s' is already deployed", definitionKind));
	}

	@Override
	public D findOne(String name) {
		return repository.findOne(name);
	}

	@Override
	public Iterable<D> findAll() {
		return repository.findAll();
	}

	@Override
	public Page<D> findAll(Pageable pageable) {
		return repository.findAll(pageable);
	}

	@Override
	public void deleteAll() {
		repository.deleteAll();
	}

	protected CrudRepository<D, String> getDefinitionRepository() {
		return repository;
	}

	protected void sendDeploymentRequests(String name, List<ModuleDeploymentRequest> requests) {
		messageSender.sendDeploymentRequests(name, requests);
	}

	protected List<ModuleDeploymentRequest> parse(String name, String config) {
		return streamParser.parse(name, config);
	}

	/**
	 * Provides basic deployment behavior, whereby running state of deployed definitions is not persisted.
	 * 
	 * @return the definition object for the given name
	 * @throws NoSuchDefinitionException if there is no definition by the given name
	 */
	protected D basicDeploy(String name) {
		Assert.hasText(name, "name cannot be blank or null");
		final D definition = getDefinitionRepository().findOne(name);
		if (definition == null) {
			throwNoSuchDefinitionException(name);
		}
		final List<ModuleDeploymentRequest> requests = parse(name, definition.getDefinition());
		sendDeploymentRequests(name, requests);
		return definition;
	}

	/**
	 * Provides basic un-deployment behavior, whereby state of deployed definitions is not dealt with.
	 */
	protected void basicUndeploy(String name) {
		D definition = getDefinitionRepository().findOne(name);
		if (definition == null) {
			throwNoSuchDefinitionException(name);
		}
		List<ModuleDeploymentRequest> requests = parse(name, definition.getDefinition());
		for (ModuleDeploymentRequest request : requests) {
			request.setRemove(true);
		}
		Collections.reverse(requests);
		sendDeploymentRequests(name, requests);
	}

	@Override
	public void delete(String name) {
		D def = getDefinitionRepository().findOne(name);
		if (def == null) {
			throwNoSuchDefinitionException(name);
		}
		beforeDelete(def);
		getDefinitionRepository().delete(def);
	}

	/**
	 * Callback method that subclasses may override to get a chance to act on definitions that are about to be deleted.
	 */
	protected void beforeDelete(D definition) {
	}

}
