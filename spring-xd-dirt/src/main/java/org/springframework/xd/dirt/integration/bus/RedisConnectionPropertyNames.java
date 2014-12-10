/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.xd.dirt.integration.bus;

/**
 * Redis message bus specific property names.
 *
 * @author Ilayaperumal Gopinathan
 */
public class RedisConnectionPropertyNames implements ConnectionPropertyNames {

	public static final String HOST = "spring.redis.host";

	public static final String PORT = "spring.redis.host";

	public static final String POOL_MAX_IDLE = "spring.redis.pool.maxIdle";

	public static final String POOL_MIN_IDLE = "spring.redis.pool.minIdle";

	public static final String POOL_MAX_ACTIVE = "spring.redis.pool.maxActive";

	public static final String POOL_MAX_WAIT = "spring.redis.pool.maxWait";

	public String[] get() {
		return new String[] {
				HOST, PORT, POOL_MAX_IDLE, POOL_MIN_IDLE, POOL_MAX_ACTIVE, POOL_MAX_WAIT
		};
	}

}
