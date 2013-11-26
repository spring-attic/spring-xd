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

package org.springframework.xd.dirt.stream.redis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.xd.dirt.module.redis.RedisModuleDependencyRepository;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.store.AbstractRedisRepository;
import org.springframework.xd.test.redis.RedisTestSupport;

/**
 * @author David Turanski
 * @author Gary Russell
 * 
 */
public class RedisStreamDefinitionRepositoryTests {

	@Rule
	public RedisTestSupport redisAvailableRule = new RedisTestSupport();

	AbstractRedisRepository<StreamDefinition, String> repository;

	@Before
	public void setUp() {
		StringRedisTemplate template = new StringRedisTemplate();
		template.setConnectionFactory(redisAvailableRule.getResource());
		template.afterPropertiesSet();
		repository = new RedisStreamDefinitionRepository(template, new RedisModuleDependencyRepository(template));
		repository.setPrefix("test.stream.definitions.");
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

	@After
	public void clearRepo() {
		repository.deleteAll();
	}
}
