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

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.BaseDefinition;
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

	protected DomainRepository<I, String> instanceRepository;

	protected AbstractInstancePersistingDeployer(PagingAndSortingRepository<D, String> definitionRespository,
			DomainRepository<I, String> instanceRepository, XDParser parser,
			ParsingContext definitionKind) {
		super(definitionRespository, parser, definitionKind);
		this.instanceRepository = instanceRepository;
	}

	@Override
	protected void beforeDelete(D definition) {
		if (instanceRepository.exists(definition.getName())) {
			undeploy(definition.getName());
		}
	}


	@Override
	public void undeploy(String name) {
		Assert.hasText(name, "name cannot be blank or null");

		final I instance = instanceRepository.findOne(name);
		if (instance == null) {
			throwNotDeployedException(name);
		}

		basicUndeploy(name);

		instanceRepository.delete(instance);

	}

	@Override
	public void deploy(String name) {

		Assert.hasText(name, "name cannot be blank or null");

		if (instanceRepository.exists(name)) {
			throwAlreadyDeployedException(name);
		}

		final D definition = basicDeploy(name);

		final I instance = makeInstance(definition);
		instanceRepository.save(instance);
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
	public void deployAll() {
		for (D definition : findAll()) {
			String name = definition.getName();
			// Make sure we deploy only the resources that are not already deployed.
			if (!instanceRepository.exists(name)) {
				deploy(name);
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
	 * Create an running instance out of the given definition;
	 */
	protected abstract I makeInstance(D definition);

}
