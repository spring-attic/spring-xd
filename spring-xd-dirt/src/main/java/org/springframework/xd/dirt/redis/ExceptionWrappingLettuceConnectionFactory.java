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
package org.springframework.xd.dirt.redis;

import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import com.lambdaworks.redis.RedisException;

/**
 * Temporary extension of {@link LettuceConnectionFactory} that addresses
 * DATAREDIS-191 LettuceConnectionFactory should throw wrapped Exceptions on
 * connection failure
 * 
 * @author Jennifer Hickey
 * 
 */
public class ExceptionWrappingLettuceConnectionFactory extends LettuceConnectionFactory {

	public ExceptionWrappingLettuceConnectionFactory() {
		super();
	}

	public ExceptionWrappingLettuceConnectionFactory(String host, int port) {
		super(host, port);
	}

	@Override
	public void initConnection() {
		try {
			super.initConnection();
		} catch (RedisException e) {
			throw new RedisConnectionFailureException("Unable to connect to Redis on " + getHostName() + ":"
					+ getPort(), e);
		}
	}

}
