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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.xd.dirt.stream.TapDefinition;
import org.springframework.xd.dirt.stream.TapInstance;
import org.springframework.xd.test.redis.RedisAvailableRule;

/**
 * @author Gunnar Hillert
 * @since 1.0
 */
public class RedisTapInstanceRepositoryTests {

	@Rule
	public RedisAvailableRule redisAvailableRule = new RedisAvailableRule();

	RedisTapDefinitionRepository tapDefinitionRepository;
	RedisTapInstanceRepository tapInstanceRepository;

	@Before
	public void setUp() {
		StringRedisTemplate template = new StringRedisTemplate();
		LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory();
		connectionFactory.setHostName("localhost");
		connectionFactory.setPort(6379);
		connectionFactory.afterPropertiesSet();
		template.setConnectionFactory(connectionFactory);
		template.afterPropertiesSet();

		tapDefinitionRepository = new RedisTapDefinitionRepository(template);
		tapDefinitionRepository.setPrefix("test.taps.definitions.");

		tapInstanceRepository = new RedisTapInstanceRepository(template, tapDefinitionRepository);
		tapInstanceRepository.setPrefix("test.taps");
	}

	@Test
	public void testInitialState() {
		assertEquals(0, tapInstanceRepository.count());
	}

	@Test
	public void testSave() {
		TapDefinition tapDefinition = new TapDefinition("tap", "test", "test | log");
		tapDefinitionRepository.save(tapDefinition);
		tapInstanceRepository.save(new TapInstance(tapDefinition));

		TapInstance saved = tapInstanceRepository.findOne("tap");

		assertEquals(tapDefinition.getName(), saved.getDefinition().getName());
		assertEquals(tapDefinition.getDefinition(), saved.getDefinition().getDefinition());

	}

	@Test
	public void testFindAll() {

		final TapDefinition tapDefinition1 = new TapDefinition("tap1", "test1", "tap@test1 | log");
		final TapDefinition tapDefinition2 = new TapDefinition("tap2", "test2", "tap@test2 | log");
		final TapDefinition tapDefinition3 = new TapDefinition("tap3", "test3", "tap@test3 | log");

		final TapDefinition savedTapDefinition1 = tapDefinitionRepository.save(tapDefinition1);
		final TapDefinition savedTapDefinition2 = tapDefinitionRepository.save(tapDefinition2);
		final TapDefinition savedTapDefinition3 = tapDefinitionRepository.save(tapDefinition3);

		final TapInstance tapInstance1 = new TapInstance(savedTapDefinition1);
		final TapInstance tapInstance2 = new TapInstance(savedTapDefinition2);
		final TapInstance tapInstance3 = new TapInstance(savedTapDefinition3);

		tapInstanceRepository.save(tapInstance1);
		tapInstanceRepository.save(tapInstance2);
		tapInstanceRepository.save(tapInstance3);

		final List<TapInstance> tapInstances = new ArrayList<TapInstance>();
		tapInstances.add(tapInstance1);
		tapInstances.add(tapInstance2);
		tapInstances.add(tapInstance3);

		int i = 0;
		for (Iterator<TapInstance> it = tapInstanceRepository.findAll().iterator(); it.hasNext();) {
			TapInstance tapInstance = it.next();
			assertEquals(tapInstances.get(i).getStartedAt(), tapInstance.getStartedAt());

			i++;
			assertEquals("tap" + i, tapInstance.getDefinition().getName());
			assertEquals("test" + i, tapInstance.getDefinition().getStreamName());
			assertEquals("tap@test" + i + " | log", tapInstance.getDefinition().getDefinition());

		}

		assertEquals(3, i);
		assertTrue(tapInstanceRepository.exists("tap1"));
		assertTrue(tapInstanceRepository.exists("tap2"));
		assertTrue(tapInstanceRepository.exists("tap3"));
		assertTrue(!tapInstanceRepository.exists("tap4"));
	}

	@After
	public void clearRepo() {
		tapDefinitionRepository.deleteAll();
		tapInstanceRepository.deleteAll();
	}
}
