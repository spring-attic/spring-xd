/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Test;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.xd.dirt.server.SingleNodeApplication;
import org.springframework.xd.dirt.server.TestApplicationBootstrap;
import org.springframework.xd.dirt.zookeeper.EmbeddedZooKeeper;


/**
 * 
 * @author David Turanski
 */
public class ZookeeperClientConnectTests {

	private TestApplicationBootstrap testApplicationBootstrap;

	private SingleNodeApplication singleNodeApplication;

	private AbstractApplicationContext containerContext;

	private AbstractApplicationContext adminContext;

	public final void setUp(String zkClientConnect, Integer zkEmbeddedServerPort) {
		System.setProperty("zk.client.connect", zkClientConnect);
		if (zkEmbeddedServerPort != null) {
			System.setProperty("zk.embedded.server.port", zkEmbeddedServerPort.toString());
		}
		this.testApplicationBootstrap = new TestApplicationBootstrap();
		this.singleNodeApplication = testApplicationBootstrap.getSingleNodeApplication();
		singleNodeApplication.run();

		this.containerContext = (AbstractApplicationContext) this.singleNodeApplication.containerContext();
		this.adminContext = (AbstractApplicationContext) this.singleNodeApplication.adminContext();
	}

	@Test
	public void testEmbeddedZooKeeper() {
		setUp("", 5555);
		String zkCLientConnect = this.containerContext.getEnvironment().getProperty("zk.client.connect");
		assertEquals("", zkCLientConnect);
		EmbeddedZooKeeper zkServer = this.containerContext.getBean(EmbeddedZooKeeper.class);
		assertEquals(5555, zkServer.getClientPort());
		this.adminContext.getBean(EmbeddedZooKeeper.class);
	}

	@Test
	public void testZooKeeperClientConnectString() {
		// String zkClientConnect = "localhost:2181, localhost:2182, localhost:2183";
		String zkClientConnect = "localhost:2181";
		setUp(zkClientConnect, null);
		String actualCLientConnect = this.containerContext.getEnvironment().getProperty("zk.client.connect");
		assertEquals(zkClientConnect, actualCLientConnect);
		try {
			this.containerContext.getBean(EmbeddedZooKeeper.class);
			fail("EmbeddedZookeeper instance should not be created with connection String set");
		}
		catch (Exception e) {

		}
		finally {
			System.clearProperty("zk.client.connect");
			System.clearProperty("zk.embedded.server.port");
		}
	}

	@After
	public final void shutDown() {
		this.singleNodeApplication.close();
	}
}
