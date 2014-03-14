/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.xd.dirt.module.ModuleDependencyRepository;
import org.springframework.xd.dirt.module.store.ZooKeeperModuleDependencyRepository;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamRepository;
import org.springframework.xd.dirt.stream.zookeeper.ZooKeeperStreamDefinitionRepository;
import org.springframework.xd.dirt.stream.zookeeper.ZooKeeperStreamRepository;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;

/**
 * @author Gunnar Hillert
 * @author Glenn Renfro
 * @author Mark Fisher
 */
@Configuration
public class StreamsControllerIntegrationWithRepositoryTestsConfig extends Dependencies {

	@Autowired
	private ZooKeeperConnection zooKeeperConnection;

	@Override
	@Bean
	public StreamDefinitionRepository streamDefinitionRepository() {
		return new ZooKeeperStreamDefinitionRepository(zooKeeperConnection, moduleDependencyRepository());
	}

	@Override
	@Bean
	public StreamRepository streamRepository() {
		return new ZooKeeperStreamRepository(zooKeeperConnection);
	}

	@Override
	@Bean
	public ModuleDependencyRepository moduleDependencyRepository() {
		return new ZooKeeperModuleDependencyRepository(zooKeeperConnection);
	}

}
