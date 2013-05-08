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
package org.springframework.xd.analytics.metrics.redis;

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.xd.analytics.metrics.core.GaugeRepository;
import org.springframework.xd.analytics.metrics.AbstractGaugeServiceTests;
import org.springframework.xd.analytics.metrics.core.GaugeService;


public class RedisGaugeServiceTests extends AbstractGaugeServiceTests {

	private RedisGaugeRepository gaugeRepository;


	@After
	@Before
	public void beforeAndAfter() {

		StringRedisTemplate stringRedisTemplate = TestUtils.getStringRedisTemplate();
		Set<String> keys = stringRedisTemplate.keys("counts." + "*");
		if (keys.size() > 0) {
			stringRedisTemplate.delete(keys);
		}

		GaugeRepository repo = getGaugeRepository();
		//TODO delete to support wildcards
		repo.delete("simpleCounter");
		repo.delete("counts.simpleCounter");
	}

	@Test
	@Ignore("Maybe issue in configuration of redis CI server environment.  Also see https://github.com/xetorthio/jedis/issues/407")
	public void testService() {
		super.simpleTest(getGaugeServiceImplementation(), getGaugeRepository());
	}

	public GaugeService getGaugeServiceImplementation() {
		return new RedisGaugeService(getGaugeRepository());
	}


	public RedisGaugeRepository getGaugeRepository() {
		gaugeRepository = new RedisGaugeRepository(TestUtils.getJedisConnectionFactory());
		return gaugeRepository;
	}

}
