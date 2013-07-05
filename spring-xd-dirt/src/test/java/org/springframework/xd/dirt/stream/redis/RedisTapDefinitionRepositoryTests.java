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
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.xd.dirt.stream.TapDefinition;

/**
 * @author David Turanski
 *
 */
public class RedisTapDefinitionRepositoryTests {

	RedisTapDefinitionRepository repository;

	@Before
	public void setUp() {
		StringRedisTemplate template = new StringRedisTemplate();
		LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory();
		connectionFactory.setHostName("localhost");
		connectionFactory.setPort(6379);
		connectionFactory.afterPropertiesSet();
		template.setConnectionFactory(connectionFactory);
		template.afterPropertiesSet();

		repository = new RedisTapDefinitionRepository(template);
	}

	@Test
	public void testInitialState() {
		assertEquals(0, repository.count());
	}

	@Test
	public void testSave() {
		TapDefinition TapDefinition = new TapDefinition("tap", "test", "test | log");
		repository.save(TapDefinition);
		TapDefinition saved = repository.findOne("tap");
		assertEquals(TapDefinition.getName(), saved.getName());
		assertEquals(TapDefinition.getDefinition(), saved.getDefinition());
	}

	@Test
	public void testFindAll() {
		repository.save(new TapDefinition("tap1", "test1", "tap@test1 | log"));
		repository.save(new TapDefinition("tap2", "test2", "tap@test2 | log"));
		repository.save(new TapDefinition("tap3", "test3", "tap@test3 | log"));

		int i = 0;
		for (Iterator<TapDefinition> it = repository.findAll().iterator(); it.hasNext();) {
			TapDefinition definition = it.next();
			i++;
			assertEquals("tap" + i, definition.getName());
			assertEquals("test" + i, definition.getStreamName());
			assertEquals("tap@test" + i + " | log", definition.getDefinition());
		}
		assertEquals(3, i);
		assertTrue(repository.exists("tap1"));
		assertTrue(repository.exists("tap2"));
		assertTrue(repository.exists("tap3"));
		assertTrue(!repository.exists("tap4"));
	}

	@After
	public void clearRepo() {
		repository.deleteAll();
	}
}
