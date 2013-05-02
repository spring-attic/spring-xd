/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.xd.test.redis;

import static org.junit.Assert.fail;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

/**
 * @author Gary Russell
 * @since 1.0
 *
 */
public class RedisAvailableRule extends TestWatcher {

	private final static Log logger = LogFactory.getLog(RedisAvailableRule.class);

	@Override
	public Statement apply(Statement base, Description description) {
		JedisConnectionFactory connectionFactory = null;
		try {
			connectionFactory = new JedisConnectionFactory();
			connectionFactory.afterPropertiesSet();
			connectionFactory.getConnection().close();
		}
		catch (Exception e) {
			logger.error("REDIS IS NOT AVAILABLE", e);
			fail("REDIS IS NOT AVAILABLE");
		}
		finally {
			if (connectionFactory != null) {
				connectionFactory.destroy();
			}
		}
		return super.apply(base, description);
	}

}
