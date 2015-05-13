/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.stream;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.BaseDefinition;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentException;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentHandler;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.store.DomainRepository;

/**
 * Base support class for deployers that know how to deal with {@link BaseInstance instances} of a
 * {@link BaseDefinition definition}.
 *
 * @param D the kind of definition this deployer deals with
 * @param I the corresponding instance type
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
public abstract class AbstractInstancePersistingDeployer<D extends BaseDefinition, I extends BaseInstance<D>> extends
		AbstractDeployer<D> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractInstancePersistingDeployer.class);

	protected final DomainRepository<I, String> instanceRepository;

	protected final DeploymentHandler deploymentHandler;

	protected AbstractInstancePersistingDeployer(ZooKeeperConnection zkConnection,
			PagingAndSortingRepository<D, String> definitionRespository,
			DomainRepository<I, String> instanceRepository, XDParser parser,
			DeploymentHandler deploymentHandler, ParsingContext definitionKind) {
		super(zkConnection, definitionRespository, parser, definitionKind);
		this.instanceRepository = instanceRepository;
		this.deploymentHandler = deploymentHandler;
	}

	@Override
	protected void beforeDelete(D definition) {
		validateBeforeDelete(definition.getName());
		if (instanceRepository.exists(definition.getName())) {
			undeploy(definition.getName());
		}
	}

	@Override
	public void undeploy(String name) {
		validateBeforeUndeploy(name);
		logger.trace("Undeploying {}", name);
		instanceRepository.delete(instanceRepository.findOne(name));
		undeployResource(name);
	}

	@Override
	public void deploy(String name, Map<String, String> properties) {
		validateBeforeDeploy(name, properties);

		final D definition = basicDeploy(name, properties);

		final I instance = makeInstance(definition);
		instanceRepository.save(instance);
		deployResource(name);
	}

	@Override
	public void undeployAll() {
		for (D definition : findAll()) {
			String name = definition.getName();
			// Make sure we un-deploy only the resources that are already deployed.
			if (instanceRepository.exists(name)) {
				undeploy(name);
			}
		}
	}

	@Override
	public void deleteAll() {
		// Make sure to un-deploy before delete.
		undeployAll();
		super.deleteAll();
	}

	/**
	 * Query deployment information about definitions whose ids range from {@code first} to {@code last}.
	 */
	public Iterable<I> deploymentInfo(String first, String last) {
		return instanceRepository.findAllInRange(first, true, last, true);
	}

	/**
	 * Query deployment information about the definition whose ID is provided.
	 */
	public BaseInstance<D> deploymentInfo(String id) {
		return instanceRepository.findOne(id);
	}

	/**
	 * Create an running instance out of the given definition;
	 */
	protected abstract I makeInstance(D definition);

	/**
	 * Deploy the deployment unit with the given name.
	 *
	 * @param deploymentUnitName the deployment unit name
	 */
	protected final void deployResource(String deploymentUnitName) {
		try {
			deploymentHandler.deploy(deploymentUnitName);
		}
		catch (Exception e) {
			throw new DeploymentException(deploymentUnitName, e);
		}
	}

	/**
	 * Un-deploy the deployment unit with the given name
	 *
	 * @param deploymentUnitName the deployment unit name
	 */
	protected final void undeployResource(String deploymentUnitName) {
		try {
			deploymentHandler.undeploy(deploymentUnitName);
		}
		catch (Exception e) {
			throw new DeploymentException(deploymentUnitName, e);
		}
	}

	@Override
	public void validateBeforeUndeploy(String name) {
		Assert.hasText(name, "name cannot be blank or null");

		D definition = getDefinitionRepository().findOne(name);
		if (definition == null) {
			throwNoSuchDefinitionException(name);
		}

		final I instance = instanceRepository.findOne(name);
		if (instance == null) {
			throwNotDeployedException(name);
		}
	}

	@Override
	public void validateBeforeDeploy(String name, Map<String, String> properties) {
		Assert.hasText(name, "name cannot be blank or null");
		Assert.notNull(properties, "properties cannot be null");
		D definition = getDefinitionRepository().findOne(name);
		if (definition == null) {
			throwNoSuchDefinitionException(name);
		}
		if (instanceRepository.exists(name)) {
			throwAlreadyDeployedException(name);
		}
	}

}
