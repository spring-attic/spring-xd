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

package org.springframework.xd.shell.command;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.junit.Test;

import org.springframework.xd.dirt.core.StreamsPath;
import org.springframework.xd.dirt.zookeeper.EmbeddedZooKeeper;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;


/**
 * 
 * @author David Turanski
 */
public class ZookeeperTests {

	@Test
	public void test() throws Exception {
		EmbeddedZooKeeper zkServer = new EmbeddedZooKeeper(5555);
		zkServer.start();
		ZooKeeperConnection zkConnection = new ZooKeeperConnection("localhost:5555");
		zkConnection.start();
		CuratorFramework client = zkConnection.getClient();
		PathChildrenCache cache = new PathChildrenCache(client, Paths.build(Paths.STREAMS), true);
		cache.getListenable().addListener(new ListenerOne());
		cache.getListenable().addListener(new ListenerTwo());
		cache.start();

		Paths.ensurePath(client, Paths.STREAMS);

		for (int i = 0; i < 100; i++) {
			client.create().creatingParentsIfNeeded().forPath(
					new StreamsPath().setStreamName("foo" + i).setModuleType("source").setModuleLabel("m" + i).build());
		}
	}

	static class ListenerOne implements PathChildrenCacheListener {

		@Override
		public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
			System.out.println("listenerOne got event " + event.getType());
		}

	}

	static class ListenerTwo implements PathChildrenCacheListener {

		@Override
		public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
			System.out.println("listenerTwo got event " + event.getType());
		}

	}
}
