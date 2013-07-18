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
import org.springframework.xd.dirt.core.AbstractDeployer;

/**
 * @author David Turanski
 * @author Luke Taylor
 */
public class TapDeployer extends AbstractDeployer<TapDefinition> {
	private final StreamDefinitionRepository streamRepository;

	public TapDeployer(TapDefinitionRepository repository, StreamDefinitionRepository streamRepository,
			DeploymentMessageSender messageSender) {
		super(repository, messageSender, "tap");
		Assert.notNull(streamRepository, "stream repository cannot be null");
		this.streamRepository = streamRepository;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.xd.dirt.core.ResourceDeployer#create(java.lang.Object)
	 */
	@Override
	public TapDefinition create(TapDefinition tapDefinition) {
		Assert.notNull(tapDefinition, "tap definition may not be null");
		if (!streamRepository.exists(tapDefinition.getStreamName())) {
			throw new NoSuchDefinitionException(tapDefinition.getStreamName(),
					"Can't tap into stream '%s' because it does not exist");
		}
		return super.create(tapDefinition);
	}

	@Override
	public void delete(String name) {
		TapDefinition def = getRepository().findOne(name);
		if (def == null) {
			throw new NoSuchDefinitionException(name,
				"Can't delete tap '%s' because it does not exist");
		}
		getRepository().delete(name);
	}

	public void undeploy(String name) {
		Assert.hasText(name, "name cannot be blank or null");
		getMessageSender().removeDeployment(name);
	}
}
