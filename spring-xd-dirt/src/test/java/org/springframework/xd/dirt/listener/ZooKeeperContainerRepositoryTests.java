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
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.UUID;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.container.ContainerAttributes;
import org.springframework.xd.dirt.container.store.ContainerRepository;
import org.springframework.xd.dirt.container.store.ZooKeeperContainerRepository;
import org.springframework.xd.dirt.listener.ZooKeeperContainerRepositoryTests.ZooKeeperContainerRepositoryTestsConfig;
import org.springframework.xd.dirt.zookeeper.EmbeddedZooKeeper;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;

/**
 * Integration test of {@link ZooKeeperContainerRepository}.
 *
 * @author Jennifer Hickey
 * @author Gary Russell
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
@ContextConfiguration(classes = ZooKeeperContainerRepositoryTestsConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class ZooKeeperContainerRepositoryTests {

	@Autowired
	private ContainerRepository containerRepository;

	@Autowired
	private ZooKeeperConnection zooKeeperConnection;

	private final String id = "test" + UUID.randomUUID().toString();

	private final String id2 = "test" + UUID.randomUUID().toString();

	private final int pid = 123;

	private final String host = "test";

	private final String ip = "127.0.0.1";

	private final Set<String> groups = StringUtils.commaDelimitedListToSet("g1,g2,g3");

	@Before
	public void setUp() throws Exception {
		try {
			zooKeeperConnection.getClient().create().creatingParentsIfNeeded().forPath(Paths.CONTAINERS);
		}
		catch (KeeperException.NodeExistsException e) {
			// ignore
		}

		ContainerAttributes containerAttributes = new ContainerAttributes(id).setPid(pid).setHost(host).setIp(ip);
		containerAttributes.put("groups", "g1,g2,g3");
		Container entity = new Container(id, containerAttributes);
		Container savedContainer = containerRepository.save(entity);
		assertNotNull(savedContainer);
		ContainerAttributes savedAttributes = savedContainer.getAttributes();
		assertEquals(id, savedAttributes.getId());
		assertEquals(pid, savedAttributes.getPid());
		assertEquals(host, savedAttributes.getHost());
		assertEquals(ip, savedAttributes.getIp());
		assertEquals(groups, savedAttributes.getGroups());

		containerAttributes = new ContainerAttributes(id2).setPid(pid).setHost(host).setIp(ip);
		entity = new Container(id2, containerAttributes);
		savedContainer = containerRepository.save(entity);
		assertNotNull(savedContainer);
		assertSavedContainer(id2);
	}

	/**
	 * Assert if the saved container exists in the {@link ContainerRepository}
	 *
	 * @param id the containerId
	 */
	private void assertSavedContainer(String id) {
		long timeout = System.currentTimeMillis() + 15000;
		boolean foundContainer = false;
		while (!foundContainer && System.currentTimeMillis() < timeout) {
			try {
				Thread.sleep(200);
				if (containerRepository.findOne(id2) != null) {
					foundContainer = true;
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		assertTrue("Container repository is not updated with the test containers", foundContainer);
	}

	@Test
	public void findContainerAttributesById() {
		Container foundContainer = containerRepository.findOne(id);
		assertNotNull(foundContainer);
		ContainerAttributes attributes = foundContainer.getAttributes();
		assertNotNull(attributes);
		assertEquals(id, attributes.getId());
		assertEquals(pid, attributes.getPid());
		assertEquals(host, attributes.getHost());
		assertEquals(ip, attributes.getIp());
		assertEquals(groups, attributes.getGroups());
	}

	@Test
	public void findContainerNoGroups() {
		Container foundContainer = containerRepository.findOne(id2);
		assertNotNull(foundContainer);
		ContainerAttributes attributes = foundContainer.getAttributes();
		assertNotNull(attributes);
		assertEquals(id2, attributes.getId());
		assertEquals(pid, attributes.getPid());
		assertEquals(host, attributes.getHost());
		assertEquals(ip, attributes.getIp());
		assertEquals(0, attributes.getGroups().size());
	}


	@Configuration
	public static class ZooKeeperContainerRepositoryTestsConfig {

		@Bean
		public EmbeddedZooKeeper embeddedZooKeeper() {
			return new EmbeddedZooKeeper();
		}

		@Bean
		public ZooKeeperConnection zooKeeperConnection() {
			return new ZooKeeperConnection("localhost:" + embeddedZooKeeper().getClientPort());
		}

		@Bean
		public ContainerRepository containerAttributesRepository() {
			return new ZooKeeperContainerRepository(zooKeeperConnection());
		}
	}

	@After
	public void tearDown() throws Exception {
		CuratorFramework client = zooKeeperConnection.getClient();
		for (String path : client.getChildren().forPath(Paths.CONTAINERS)) {
			client.delete().deletingChildrenIfNeeded().forPath(Paths.build(Paths.CONTAINERS, path));
		}
	}

}
