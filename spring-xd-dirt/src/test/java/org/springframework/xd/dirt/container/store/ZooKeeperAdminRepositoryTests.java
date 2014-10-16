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
import org.springframework.xd.dirt.cluster.Admin;
import org.springframework.xd.dirt.cluster.AdminAttributes;
import org.springframework.xd.dirt.container.store.ZooKeeperAdminRepositoryTests.ZooKeeperAdminRepositoryTestsConfig;
import org.springframework.xd.dirt.zookeeper.EmbeddedZooKeeper;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperAccessException;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;

/**
 * Tests for {@link ZooKeeperAdminRepository}.
 *
 * @author Janne Valkealahti
 */
@ContextConfiguration(classes = ZooKeeperAdminRepositoryTestsConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class ZooKeeperAdminRepositoryTests {

	@Autowired
	private AdminRepository adminRepository;

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

		AdminAttributes adminAttributes = new AdminAttributes(id).setPid(pid).setHost(host).setIp(ip);
		Admin entity = new Admin(id, adminAttributes);
		Admin savedAdmin = adminRepository.save(entity);
		assertNotNull(savedAdmin);
		AdminAttributes savedAttributes = savedAdmin.getAttributes();
		assertEquals(id, savedAttributes.getId());
		assertEquals(pid, savedAttributes.getPid());
		assertEquals(host, savedAttributes.getHost());
		assertEquals(ip, savedAttributes.getIp());

		adminAttributes = new AdminAttributes(id2).setPid(pid).setHost(host).setIp(ip);
		entity = new Admin(id2, adminAttributes);
		savedAdmin = adminRepository.save(entity);
		assertNotNull(savedAdmin);
		assertSavedAdmin(id2, adminAttributes);
	}

	@Test
	public void findContainerAttributesById() {
		Admin foundAdmin = adminRepository.findOne(id);
		assertNotNull(foundAdmin);
		AdminAttributes attributes = foundAdmin.getAttributes();
		assertNotNull(attributes);
		assertEquals(id, attributes.getId());
		assertEquals(pid, attributes.getPid());
		assertEquals(host, attributes.getHost());
		assertEquals(ip, attributes.getIp());
	}

	@Test
	public void updateContainerAttributes() {
		Admin foundAdmin = adminRepository.findOne(id);
		assertNotNull(foundAdmin);
		AdminAttributes adminAttributes = new AdminAttributes(id).setPid(12345).setHost("randomHost").setIp(
				"randomIP");
		Admin entity = new Admin(id, adminAttributes);
		adminRepository.update(entity);
		assertSavedAdmin(id, adminAttributes);
	}

	@Test
	public void updateNonExistingContainer() {
		exception.expect(ZooKeeperAccessException.class);
		exception.expectMessage("Could not find admin with id " + id + 10);
		AdminAttributes adminAttributes = new AdminAttributes(id + 10).setPid(12345).setHost("randomHost").setIp(
				"randomIP");
		Admin entity = new Admin(id + 10, adminAttributes);
		adminRepository.update(entity);
	}

	/**
	 * Assert if the saved container exists in the {@link ContainerRepository}
	 *
	 * @param id the containerId
	 * @param adminAttributes the admin attributes
	 */
	private void assertSavedAdmin(String id, AdminAttributes adminAttributes) {
		long timeout = System.currentTimeMillis() + 15000;
		boolean foundAdmin = false;
		while (!foundAdmin && System.currentTimeMillis() < timeout) {
			try {
				Thread.sleep(200);
				Admin admin = adminRepository.findOne(id);
				if (admin != null && compareAdminAttributes(admin.getAttributes(), adminAttributes)) {
					foundAdmin = true;
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		assertTrue("Admin repository is not updated with the test containers", foundAdmin);
	}

	/**
	 * Check if the admin attributes equal.
	 *
	 * @param attr1 admin attributes
	 * @param attr2 admin attributes
	 * @return true if the admin attributes match
	 */
	private boolean compareAdminAttributes(AdminAttributes attr1, AdminAttributes attr2) {
		return (attr1.getId().equals(attr2.getId()) &&
				attr1.getHost().equals(attr2.getHost()) &&
				attr1.getIp().equals(attr2.getIp()) && (attr1.getPid() == attr2.getPid()));
	}

	@Configuration
	public static class ZooKeeperAdminRepositoryTestsConfig {

		@Bean
		public EmbeddedZooKeeper embeddedZooKeeper() {
			return new EmbeddedZooKeeper();
		}

		@Bean
		public ZooKeeperConnection zooKeeperConnection() {
			return new ZooKeeperConnection("localhost:" + embeddedZooKeeper().getClientPort());
		}

		@Bean
		public AdminRepository adminRepository() {
			return new ZooKeeperAdminRepository(zooKeeperConnection());
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
