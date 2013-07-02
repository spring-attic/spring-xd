/*
 * Copyright 2011-2013 the original author or authors.
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

package org.springframework.xd.analytics.metrics.redis;

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.xd.analytics.metrics.AbstractAggregateCounterTests;
import org.springframework.xd.analytics.metrics.common.ServicesConfig;

@ContextConfiguration(classes=ServicesConfig.class, loader=AnnotationConfigContextLoader.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisAggregateCounterTests extends AbstractAggregateCounterTests {

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	@Before
	@After
	public void beforeAndAfter() {
		RedisAggregateCounterService redisService = (RedisAggregateCounterService)counterService;
		Set<String> keys = stringRedisTemplate.opsForSet().members(redisService.getKeyForAllCounterNames());
		if (keys.size() > 0) {
			stringRedisTemplate.delete(keys);
		}
		stringRedisTemplate.delete(redisService.getKeyForAllCounterNames());
		stringRedisTemplate.delete(redisService.getKeyForAllRootCounterNames());
	}

}
