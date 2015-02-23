/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.stream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.MessageChannel;
import org.springframework.xd.dirt.integration.bus.redis.RedisTestMessageBus;
import org.springframework.xd.test.redis.RedisTestSupport;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class RedisSingleNodeStreamDeploymentIntegrationTests extends
		AbstractDistributedTransportSingleNodeStreamDeploymentIntegrationTests {

	@ClassRule
	public static RedisTestSupport redisAvailableRule = new RedisTestSupport();

	@BeforeClass
	public static void setUp() {
		setUp("redis");
	}

	@ClassRule
	public static ExternalResource initializeRedisTestMessageBus = new ExternalResource() {

		@Override
		protected void before() {
			if (testMessageBus == null || !(testMessageBus instanceof RedisTestMessageBus)) {
				testMessageBus = new RedisTestMessageBus(redisAvailableRule.getResource());
			}
		}
	};

	@AfterClass
	public static void cleanup() {
		singleNodeApplication.close();
	}

	@Override
	protected void verifyOnDemandQueues(MessageChannel y3, MessageChannel z3, Map<String, Object> initialTransportState) {
		StringRedisTemplate template = new StringRedisTemplate(redisAvailableRule.getResource());
		String y = template.boundListOps("queue.queue:y").rightPop();
		assertNotNull(y);
		assertTrue(y.endsWith("y")); // bus headers
		String z = template.boundListOps("queue.queue:z").rightPop();
		assertNotNull(z);
		assertTrue(z.endsWith("z")); // bus headers
	}


}
