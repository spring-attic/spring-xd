/*
 * Copyright 2013-2014 the original author or authors.
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

import static org.springframework.xd.dirt.stream.ParsingContext.stream;

import org.springframework.xd.dirt.server.admin.deployment.DeploymentHandler;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;

/**
 * Default implementation of {@link ZKStreamDeployer} that uses provided
 * {@link StreamDefinitionRepository} and {@link StreamRepository} to
 * persist stream deployment and undeployment requests.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Andy Clement
 * @author Eric Bottard
 * @author Gunnar Hillert
 * @author Patrick Peralta
 */
public class ZKStreamDeployer extends AbstractInstancePersistingZKDeployer<StreamDefinition, Stream> {

	/**
	 * Stream definition parser.
	 */
	private final XDParser parser;

	/**
	 * Construct a StreamDeployer.
	 *
	 * @param zkConnection       ZooKeeper connection
	 * @param repository         repository for stream definitions
	 * @param streamRepository   repository for stream instances
	 * @param parser             stream definition parser
	 */
	public ZKStreamDeployer(ZooKeeperConnection zkConnection, StreamDefinitionRepository repository,
			StreamRepository streamRepository, XDParser parser, DeploymentHandler deploymentHandler) {
		super(zkConnection, repository, streamRepository, parser, deploymentHandler, stream);
		this.parser = parser;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Stream makeInstance(StreamDefinition definition) {
		return new Stream(definition);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected StreamDefinition createDefinition(String name, String definition) {
		return new StreamDefinition(name, definition);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getDeploymentPath(StreamDefinition definition) {
		return Paths.build(Paths.STREAM_DEPLOYMENTS, definition.getName());
	}

}
