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

package org.springframework.integration.x.rabbit;

import org.junit.Rule;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.integration.x.channel.registry.AbstractChannelRegistryTests;
import org.springframework.integration.x.channel.registry.ChannelRegistry;
import org.springframework.xd.test.rabbit.RabbitAvailableRule;

/**
 * @author Mark Fisher
 */
public class RabbitChannelRegistryTests extends AbstractChannelRegistryTests {

	@Rule
	public RabbitAvailableRule rabbitAvailableRule = new RabbitAvailableRule();

	@Override
	protected ChannelRegistry getRegistry() throws Exception {
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory("localhost");
		RabbitChannelRegistry registry = new RabbitChannelRegistry(connectionFactory);
		return registry;
	}

}
