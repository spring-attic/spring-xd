/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.config;

import org.apache.curator.RetryPolicy;
import org.apache.curator.RetrySleeper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnectionConfigurer;


/**
 * Provide a {@link ZooKeeperConnectionConfigurer} bean via an XD extension to configure a {@link RetryPolicy}
 * 
 * @author David Turanski
 */
@Configuration
public class ZooKeeperConnectionConfiguration {

	@Bean
	ZooKeeperConnectionConfigurer zooKeeperConnectionConfigurer() {
		return new ZooKeeperConnectionConfigurer() {

			@Override
			public void configureZooKeeperConnection(ZooKeeperConnection zooKeeperConnection) {
				zooKeeperConnection.setRetryPolicy(new RetryPolicy() {

					@Override
					public boolean allowRetry(int retryCount, long elapsedTimeMs, RetrySleeper sleeper) {
						return false;
					}
				});
			}
		};
	}
}
