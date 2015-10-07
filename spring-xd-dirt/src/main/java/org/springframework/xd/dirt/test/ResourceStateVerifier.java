/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.test;

import java.util.Arrays;
import java.util.List;

import org.springframework.xd.dirt.core.BaseDefinition;
import org.springframework.xd.dirt.core.DeploymentStatusRepository;
import org.springframework.xd.dirt.core.DeploymentUnitStatus;
import org.springframework.xd.dirt.core.DeploymentUnitStatus.State;
import org.springframework.xd.store.DomainRepository;

/**
 * Verifies the state of resources (job or stream) by polling repository state until the
 * resource is in the expected state or the operation times out. For use with
 * {@link org.springframework.xd.dirt.server.singlenode.SingleNodeApplication} testing.
 *
 * @see org.springframework.xd.dirt.test.SingleNodeIntegrationTestSupport
 * @author David Turanski
 * @since 1.0
 */
public class ResourceStateVerifier {

	private static final long STATE_CHANGE_WAIT_TIME = 50;

	private static final long STATE_CHANGE_TIMEOUT = 35000;

	private final DeploymentStatusRepository<?, String> deploymentStatusRepository;

	private final DomainRepository<? extends BaseDefinition, String> domainRepository;

	/**
	 *
	 * @param deploymentStatusRepository the repository that tracks deployment status
	 * @param domainRepository the resource definition repository
	 */
	public ResourceStateVerifier(DeploymentStatusRepository<?, String> deploymentStatusRepository,
			DomainRepository<? extends BaseDefinition, String> domainRepository) {
		this.deploymentStatusRepository = deploymentStatusRepository;
		this.domainRepository = domainRepository;
	}

	/**
	 * Wait for a named resource to be completely deployed.
	 * @param resourceName the name of the resource being deployed
	 * @return the deployment status, either State.deployed or the current State
	 *         when this operation timed out.
	 */
	public State waitForDeploy(String resourceName) {
		return waitForDeploy(resourceName, false);
	}

	/**
	 * Wait for a named resource to be deployed.
	 * @param resourceName the name of the resource being deployed
	 * @param allowIncomplete if true, will return on State.incomplete or State.deployed
	 * @return one of the expected States or the current State when this operation timed out.
	 */
	public State waitForDeploy(String resourceName, boolean allowIncomplete) {
		if (allowIncomplete) {
			return waitForDeployState(resourceName, State.deployed, State.incomplete);
		}
		return waitForDeployState(resourceName, State.deployed);
	}

	/**
	 * Wait for a named resource to be undeployed.
	 * @param resourceName the name of the resource being undeployed
	 * @return either State.undeployed or the current state when the operation timed out.
	 */
	public State waitForUndeploy(String resourceName) {
		return waitForDeployState(resourceName, State.undeployed);
	}

	/**
	 * Wait for a resource definition to be created.
	 * @param resourceName the name of the resource being created.
	 * @return true if the resource exists; false otherwise.
	 */
	public boolean waitForCreate(String resourceName) {
		long waitTime = 0;
		boolean exists;
		while (!(exists = domainRepository.exists(resourceName)) && waitTime < STATE_CHANGE_TIMEOUT) {
			try {
				Thread.sleep(STATE_CHANGE_WAIT_TIME);
				waitTime += STATE_CHANGE_WAIT_TIME;
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		return exists;
	}

	/**
	 * Wait for a resource definition to be destroyed.
	 * @param resourceName the name of the resource being destroyed.
	 * @return true if the resource does not exist; false otherwise.
	 */
	public boolean waitForDestroy(String resourceName) {
		long waitTime = 0;
		boolean exists;
		while ((exists = domainRepository.exists(resourceName)) && waitTime < STATE_CHANGE_TIMEOUT) {
			try {
				Thread.sleep(STATE_CHANGE_WAIT_TIME);
				waitTime += STATE_CHANGE_WAIT_TIME;
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		return exists;

	}

	/**
	 * Wait for the State to change to one or more expected target states.
	 */
	State waitForDeployState(String resourceName, State... targetStates) {
		DeploymentUnitStatus.State currentState = deploymentStatusRepository.getDeploymentStatus(resourceName).getState();
		long waitTime = 0;
		List<State> targetStateList = Arrays.asList(targetStates);
		while (!targetStateList.contains(currentState) && waitTime < STATE_CHANGE_TIMEOUT) {
			try {
				Thread.sleep(STATE_CHANGE_WAIT_TIME);
				currentState = deploymentStatusRepository.getDeploymentStatus(resourceName).getState();
				waitTime += STATE_CHANGE_WAIT_TIME;
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		return currentState;
	}
}
