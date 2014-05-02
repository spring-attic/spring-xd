/*
 * Copyright 2011-2014 the original author or authors.
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
import java.util.List;

import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.BaseDefinition;
import org.springframework.xd.dirt.core.ResourceDeployer;
import org.springframework.xd.dirt.module.ModuleDescriptor;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.module.ModuleDefinition;

/**
 * Abstract implementation of the @link {@link org.springframework.xd.dirt.core.ResourceDeployer} interface. It provides
 * the basic support for calling CrudRepository methods and sending deployment messages.
 *
 * @author Luke Taylor
 * @author Mark Pollack
 * @author Eric Bottard
 * @author Andy Clement
 * @author David Turanski
 */
public abstract class AbstractDeployer<D extends BaseDefinition> implements ResourceDeployer<D> {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractDeployer.class);

	private final PagingAndSortingRepository<D, String> repository;

	private final ZooKeeperConnection zkConnection;

	protected final XDParser streamParser;

	/**
	 * Used in exception messages as well as indication to the parser.
	 */
	protected final ParsingContext definitionKind;

	protected AbstractDeployer(ZooKeeperConnection zkConnection, PagingAndSortingRepository<D, String> repository,
			XDParser parser, ParsingContext parsingContext) {
		Assert.notNull(zkConnection, "ZooKeeper connection cannot be null");
		Assert.notNull(repository, "Repository cannot be null");
		Assert.notNull(parsingContext, "Entity type kind cannot be null");
		this.zkConnection = zkConnection;
		this.repository = repository;
		this.definitionKind = parsingContext;
		this.streamParser = parser;
	}

	@Override
	public D save(D definition) {
		Assert.notNull(definition, "Definition may not be null");
		if (repository.findOne(definition.getName()) != null) {
			throwDefinitionAlreadyExistsException(definition);
		}
		List<ModuleDescriptor> moduleDeploymentRequests = streamParser.parse(definition.getName(),
				definition.getDefinition(), definitionKind);
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
	private List<ModuleDefinition> createModuleDefinitions(List<ModuleDescriptor> moduleDeploymentRequests) {
		List<ModuleDefinition> moduleDefinitions = new ArrayList<ModuleDefinition>(moduleDeploymentRequests.size());

		for (ModuleDescriptor moduleDeploymentRequest : moduleDeploymentRequests) {
			ModuleDefinition moduleDefinition = new ModuleDefinition(moduleDeploymentRequest.getModuleName(),
					moduleDeploymentRequest.getType());
			moduleDefinitions.add(moduleDefinition);
		}

		return moduleDefinitions;
	}

	/**
	 * Return the ZooKeeper connection.
	 *
	 * @return the ZooKeeper connection
	 */
	protected ZooKeeperConnection getZooKeeperConnection() {
		return zkConnection;
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
		for (D d : findAll()) {
			delete(d.getName());
		}
	}

	protected CrudRepository<D, String> getDefinitionRepository() {
		return repository;
	}

	/**
	 * Provides basic deployment behavior, whereby running state of deployed definitions is not persisted.
	 *
	 * @return the definition object for the given name
	 * @throws NoSuchDefinitionException if there is no definition by the given name
	 */
	protected D basicDeploy(String name, String properties) {
		Assert.hasText(name, "name cannot be blank or null");
		LOG.trace("Deploying {}", name);

		final D definition = getDefinitionRepository().findOne(name);
		if (definition == null) {
			throwNoSuchDefinitionException(name);
		}
		try {
			byte[] data = properties != null ? properties.getBytes("UTF-8") : null;
			zkConnection.getClient().create().creatingParentsIfNeeded().forPath(getDeploymentPath(definition), data);
		}
		catch (KeeperException.NodeExistsException e) {
			// todo: is this the right exception to throw here?
			throw new IllegalStateException(String.format("Stream %s is already deployed", name));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return definition;
	}

	/**
	 * Provides basic un-deployment behavior, whereby state of deployed definitions is not dealt with.
	 */
	protected void basicUndeploy(String name) {
		Assert.hasText(name, "name cannot be blank or null");
		LOG.trace("Undeploying {}", name);

		D definition = getDefinitionRepository().findOne(name);
		if (definition == null) {
			throwNoSuchDefinitionException(name);
		}

		try {
			zkConnection.getClient().delete().deletingChildrenIfNeeded().forPath(getDeploymentPath(definition));
		}
		catch (KeeperException.NoNodeException e) {
			// ignore; this has already been undeployed
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected abstract D createDefinition(String name, String definition);

	/**
	 * Return the ZooKeeper path used for deployment requests for the
	 * given definition.
	 *
	 * @param definition definition for which to obtain path
	 *
	 * @return ZooKeeper path for deployment requests
	 */
	protected abstract String getDeploymentPath(D definition);

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
