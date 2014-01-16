/*
 * Copyright 2013 the original author or authors.
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.xd.test.redis.RedisTestSupport;

/**
 * @author Mark Fisher
 */
public class RedisSingleNodeStreamDeploymentIntegrationTests extends AbstractSingleNodeStreamDeploymentIntegrationTests {

	@ClassRule
	public static RedisTestSupport redisAvailableRule = new RedisTestSupport();

	@BeforeClass
	public static void setUp() {
		setUp("redis");
	}

	@AfterClass
	public static void cleanup() {
		if (context != null) {
			StringRedisTemplate template = context.getBean(StringRedisTemplate.class);
			String queueDeployer = context.getEnvironment().resolvePlaceholders(XD_DEPLOYER_PLACEHOLDER);
			template.delete(queueDeployer);
		}
	}
}
