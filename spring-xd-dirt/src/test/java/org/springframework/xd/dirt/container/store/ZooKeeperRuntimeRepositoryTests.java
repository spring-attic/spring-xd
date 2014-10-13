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

package org.springframework.xd.dirt.container.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.dirt.cluster.ContainerRuntime;
import org.springframework.xd.dirt.cluster.RuntimeAttributes;
import org.springframework.xd.dirt.container.store.ZooKeeperRuntimeRepositoryTests.ZooKeeperRuntimeRepositoryTestsConfig;
import org.springframework.xd.dirt.zookeeper.EmbeddedZooKeeper;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperAccessException;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;

/**
 * Tests for {@link ZooKeeperRuntimeRepository}.
 *
 * @author Janne Valkealahti
 */
@ContextConfiguration(classes = ZooKeeperRuntimeRepositoryTestsConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class ZooKeeperRuntimeRepositoryTests {

	@Autowired
	private RuntimeRepository runtimeRepository;

	@Autowired
	private ZooKeeperConnection zooKeeperConnection;

	private final String id = "test" + UUID.randomUUID().toString();

	private final String id2 = "test" + UUID.randomUUID().toString();

	private final int pid = 123;

	private final String host = "test";

	private final String ip = "127.0.0.1";

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Before
	public void setUp() throws Exception {
		try {
			zooKeeperConnection.getClient().create().creatingParentsIfNeeded().forPath(Paths.ADMINS);
		}
		catch (KeeperException.NodeExistsException e) {
			// ignore
		}

		RuntimeAttributes runtimeAttributes = new RuntimeAttributes(id).setPid(pid).setHost(host).setIp(ip);
		ContainerRuntime entity = new ContainerRuntime(id, runtimeAttributes);
		ContainerRuntime savedContainer = runtimeRepository.save(entity);
		assertNotNull(savedContainer);
		RuntimeAttributes savedAttributes = savedContainer.getAttributes();
		assertEquals(id, savedAttributes.getId());
		assertEquals(pid, savedAttributes.getPid());
		assertEquals(host, savedAttributes.getHost());
		assertEquals(ip, savedAttributes.getIp());

		runtimeAttributes = new RuntimeAttributes(id2).setPid(pid).setHost(host).setIp(ip);
		entity = new ContainerRuntime(id2, runtimeAttributes);
		savedContainer = runtimeRepository.save(entity);
		assertNotNull(savedContainer);
		assertSavedContainer(id2, runtimeAttributes);
	}

	@Test
	public void findContainerAttributesById() {
		ContainerRuntime foundContainer = runtimeRepository.findOne(id);
		assertNotNull(foundContainer);
		RuntimeAttributes attributes = foundContainer.getAttributes();
		assertNotNull(attributes);
		assertEquals(id, attributes.getId());
		assertEquals(pid, attributes.getPid());
		assertEquals(host, attributes.getHost());
		assertEquals(ip, attributes.getIp());
	}

	@Test
	public void updateContainerAttributes() {
		ContainerRuntime foundContainer = runtimeRepository.findOne(id);
		assertNotNull(foundContainer);
		RuntimeAttributes containerAttributes = new RuntimeAttributes(id).setPid(12345).setHost("randomHost").setIp(
				"randomIP");
		containerAttributes.put("groups", "test1,test2");
		ContainerRuntime entity = new ContainerRuntime(id, containerAttributes);
		runtimeRepository.update(entity);
		assertSavedContainer(id, containerAttributes);
	}

	@Test
	public void updateNonExistingContainer() {
		exception.expect(ZooKeeperAccessException.class);
		exception.expectMessage("Could not find admin with id " + id + 10);
		RuntimeAttributes containerAttributes = new RuntimeAttributes(id + 10).setPid(12345).setHost("randomHost").setIp(
				"randomIP");
		ContainerRuntime entity = new ContainerRuntime(id + 10, containerAttributes);
		runtimeRepository.update(entity);
	}

	/**
	 * Assert if the saved container exists in the {@link ContainerRepository}
	 *
	 * @param id the containerId
	 */
	private void assertSavedContainer(String id, RuntimeAttributes runtimeAttributes) {
		long timeout = System.currentTimeMillis() + 15000;
		boolean foundContainer = false;
		while (!foundContainer && System.currentTimeMillis() < timeout) {
			try {
				Thread.sleep(200);
				ContainerRuntime container = runtimeRepository.findOne(id);
				if (container != null && compareContainerAttributes(container.getAttributes(), runtimeAttributes)) {
					foundContainer = true;
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		assertTrue("Container repository is not updated with the test containers", foundContainer);
	}

	/**
	 * Check if the container attributes equal.
	 *
	 * @param attr1 container attributes
	 * @param attr2 container attributes
	 * @return true if the container attributes match
	 */
	private boolean compareContainerAttributes(RuntimeAttributes attr1, RuntimeAttributes attr2) {
		return (attr1.getId().equals(attr2.getId()) &&
				attr1.getHost().equals(attr2.getHost()) &&
				attr1.getIp().equals(attr2.getIp()) && (attr1.getPid() == attr2.getPid()));
	}

	@Configuration
	public static class ZooKeeperRuntimeRepositoryTestsConfig {

		@Bean
		public EmbeddedZooKeeper embeddedZooKeeper() {
			return new EmbeddedZooKeeper();
		}

		@Bean
		public ZooKeeperConnection zooKeeperConnection() {
			return new ZooKeeperConnection("localhost:" + embeddedZooKeeper().getClientPort());
		}

		@Bean
		public RuntimeRepository runtimeRepository() {
			return new ZooKeeperRuntimeRepository(zooKeeperConnection());
		}

	}

	@After
	public void tearDown() throws Exception {
		CuratorFramework client = zooKeeperConnection.getClient();
		for (String path : client.getChildren().forPath(Paths.ADMINS)) {
			client.delete().deletingChildrenIfNeeded().forPath(Paths.build(Paths.ADMINS, path));
		}
	}

}
