/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.store;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;

import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.xd.test.redis.RedisTestSupport;

/**
 * Tests for {@link AbstractRedisRepository}.
 * 
 * @author Eric Bottard
 */
public class AbstractRedisRepositoryTests extends BaseRepositoryTests<AbstractRedisRepository<String, Integer>> {

	private static JedisConnectionFactory connectionFactory;

	@Rule
	public RedisTestSupport redisAvailableRule = new RedisTestSupport();

	@BeforeClass
	public static void setupConnection() {
		connectionFactory = new JedisConnectionFactory();
		connectionFactory.afterPropertiesSet();
	}

	@AfterClass
	public static void cleanConnection() {
		if (connectionFactory != null) {
			connectionFactory.destroy();
		}
	}

	@Override
	protected AbstractRedisRepository<String, Integer> createRepository() {
		return new AbstractRedisRepository<String, Integer>("_test", new StringRedisTemplate(connectionFactory)) {

			@Override
			protected String serializeId(Integer id) {
				// Need padding to be comparable
				return String.format("%05d", id);
			}

			@Override
			protected String serialize(String entity) {
				return entity;
			}

			@Override
			protected Integer keyFor(String entity) {
				return NUMBERS.indexOf(entity);
			}

			@Override
			protected Integer deserializeId(String string) {
				return Integer.valueOf(string);
			}

			@Override
			protected String deserialize(Integer id, String v) {
				return v;
			}
		};
	}

}
