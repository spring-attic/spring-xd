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

package org.springframework.xd.dirt.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.dirt.container.ContainerMetadata;
import org.springframework.xd.dirt.container.store.ContainerMetadataRepository;
import org.springframework.xd.dirt.container.store.ZooKeeperContainerMetadataRepository;
import org.springframework.xd.dirt.listener.ZooKeeperContainerMetadataRepositoryTests.ZooKeeperContainerMetadataRepositoryTestsConfig;
import org.springframework.xd.dirt.zookeeper.EmbeddedZooKeeper;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;

/**
 * Integration test of {@link ZooKeeperContainerMetadataRepository}.
 * 
 * @author Jennifer Hickey
 * @author Gary Russell
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
@ContextConfiguration(classes = ZooKeeperContainerMetadataRepositoryTestsConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class ZooKeeperContainerMetadataRepositoryTests {

	@Autowired
	private ContainerMetadataRepository containerMetadataRepository;

	@Autowired
	private ZooKeeperConnection zooKeeperConnection;

	private final String id = "test" + UUID.randomUUID().toString();

	private final int pid = 123;

	private final String host = "test";

	private final String ip = "127.0.0.1";

	@Before
	public void setUp() throws Exception {
		zooKeeperConnection.getClient().create().creatingParentsIfNeeded().forPath(Paths.CONTAINERS);
		ContainerMetadata entity = new ContainerMetadata(id, pid, host, ip);
		ContainerMetadata saved = containerMetadataRepository.save(entity);
		assertNotNull(saved);
		assertEquals(id, saved.getId());
		assertEquals(pid, saved.getPid());
		assertEquals(host, saved.getHost());
		assertEquals(ip, saved.getIp());
	}

	@Test
	public void findContainerMetadataById() {
		ContainerMetadata found = containerMetadataRepository.findOne(id);
		assertNotNull(found);
		assertEquals(id, found.getId());
		assertEquals(pid, found.getPid());
		assertEquals(host, found.getHost());
		assertEquals(ip, found.getIp());
	}


	@Configuration
	public static class ZooKeeperContainerMetadataRepositoryTestsConfig {

		@Bean
		public EmbeddedZooKeeper embeddedZooKeeper() {
			return new EmbeddedZooKeeper();
		}

		@Bean
		public ZooKeeperConnection zooKeeperConnection() {
			return new ZooKeeperConnection("localhost:" + embeddedZooKeeper().getClientPort());
		}

		@Bean
		public ContainerMetadataRepository runtimeContainerInfoRepository() {
			return new ZooKeeperContainerMetadataRepository(zooKeeperConnection());
		}
	}

}
