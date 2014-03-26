/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.stream.zookeeper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.xd.dirt.module.memory.InMemoryModuleDependencyRepository;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.zookeeper.EmbeddedZooKeeper;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;

/**
 * Unit tests for {@link ZooKeeperStreamDefinitionRepository}.
 * 
 * @author Eric Bottard
 * @author David Turanski
 * @author Gary Russell
 * @author Mark Fisher
 */
public class ZooKeeperStreamDefinitionRepositoryTests {

	private static EmbeddedZooKeeper embeddedZooKeeper = new EmbeddedZooKeeper();

	private ZooKeeperStreamDefinitionRepository repository;

	private static ZooKeeperConnection zkConnection;

	@BeforeClass
	public static void initZooKeeper() {
		embeddedZooKeeper.start();
		zkConnection = new ZooKeeperConnection("localhost:"
				+ embeddedZooKeeper.getClientPort());
		zkConnection.start();
	}

	@Before
	public void createRepository() throws Exception {
		this.repository = new ZooKeeperStreamDefinitionRepository(zkConnection,
				new InMemoryModuleDependencyRepository());

		Paths.ensurePath(zkConnection.getClient(), Paths.STREAMS);
	}

	@After
	public void shutdownRepository() {
		if (repository != null) {
			repository.deleteAll();
		}

	}

	@AfterClass
	public static void stopZooKeeper() {
		zkConnection.stop();
		embeddedZooKeeper.stop();
	}

	@Test
	public void newlyCreatedRepo() {
		Assert.assertEquals(0, repository.count());
		Assert.assertNull(repository.findOne("some"));
		Assert.assertFalse(repository.findAll().iterator().hasNext());
		Assert.assertFalse(repository.exists("some"));
	}

	@Test
	public void simpleAdding() {
		StreamDefinition stream = new StreamDefinition("some", "http | hdfs");
		repository.save(stream);

		Assert.assertTrue(repository.exists("some"));
		Assert.assertEquals(1, repository.count());
		Assert.assertNotNull(repository.findOne("some"));
		Iterator<StreamDefinition> iterator = repository.findAll().iterator();
		Assert.assertEquals(stream, iterator.next());
		Assert.assertFalse(iterator.hasNext());
	}

	@Test
	public void multipleAdding() {
		StreamDefinition one = new StreamDefinition("one", "http | hdfs");
		StreamDefinition two = new StreamDefinition("two", "tcp | file");
		repository.save(Arrays.asList(one, two));

		Assert.assertTrue(repository.exists("one"));
		Assert.assertTrue(repository.exists("two"));
	}

	@Test
	public void multipleFinding() {
		StreamDefinition one = new StreamDefinition("one", "http | hdfs");
		repository.save(one);

		Iterator<StreamDefinition> iterator = repository.findAll(Arrays.asList("one", "notthere")).iterator();
		Assert.assertEquals(one, iterator.next());
		Assert.assertFalse(iterator.hasNext());
	}

	@Test
	public void deleteSimple() {
		StreamDefinition one = new StreamDefinition("one", "http | hdfs");
		StreamDefinition two = new StreamDefinition("two", "tcp | file");
		repository.save(Arrays.asList(one, two));

		repository.delete("one");
		Assert.assertFalse(repository.exists("one"));

		repository.delete(two);
		Assert.assertFalse(repository.exists("two"));
	}

	@Test
	public void deleteMultiple() {
		StreamDefinition one = new StreamDefinition("one", "http | hdfs");
		StreamDefinition two = new StreamDefinition("two", "tcp | file");
		StreamDefinition three = new StreamDefinition("three", "http | file");
		repository.save(Arrays.asList(one, two, three));

		repository.delete(Arrays.asList(one));
		Assert.assertFalse(repository.exists("one"));
		Assert.assertTrue(repository.exists("two"));

		repository.deleteAll();
		Assert.assertEquals(0, repository.count());
	}

	@Test
	public void override() {
		StreamDefinition one = new StreamDefinition("one", "http | hdfs");
		repository.save(one);
		Assert.assertEquals("http | hdfs", repository.findOne("one").getDefinition());

		StreamDefinition otherone = new StreamDefinition("one", "time | file");
		repository.save(otherone);
		Assert.assertEquals(1, repository.count());
		Assert.assertEquals("time | file", repository.findOne("one").getDefinition());
	}

	@Test
	public void testInitialState() {
		assertEquals(0, repository.count());
	}

	@Test
	public void testSave() {
		StreamDefinition streamDefinition = new StreamDefinition("test", "time | log");
		repository.save(streamDefinition);
		StreamDefinition saved = repository.findOne("test");
		assertEquals(streamDefinition.getName(), saved.getName());
		assertEquals(streamDefinition.getDefinition(), saved.getDefinition());
	}

	@Test
	public void testFindAll() {
		repository.save(new StreamDefinition("test1", "time | log"));
		repository.save(new StreamDefinition("test2", "time | log"));
		repository.save(new StreamDefinition("test3", "time | log"));
		int i = 0;
		for (Iterator<StreamDefinition> it = repository.findAll().iterator(); it.hasNext();) {
			it.next();
			i++;
		}
		assertEquals(3, i);
		assertTrue(repository.exists("test1"));
		assertTrue(repository.exists("test2"));
		assertTrue(repository.exists("test3"));
		assertTrue(!repository.exists("test4"));
	}

	@Test
	public void testDelete() {
		StreamDefinition streamDefinition = new StreamDefinition("test", "time | log");
		repository.save(streamDefinition);
		StreamDefinition saved = repository.findOne("test");
		repository.delete(saved);
		assertNull(repository.findOne("test"));
	}

}
