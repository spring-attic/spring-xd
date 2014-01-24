/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.x.bus;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.integration.x.bus.serializer.MultiTypeCodec;
import org.springframework.integration.x.rabbit.RabbitMessageBus;


/**
 * Test support class for {@link RabbitMessageBus}.
 * 
 * @author Ilayaperumal Gopinathan
 */
public class RabbitTestMessageBus extends AbstractTestMessageBus {

	private final RabbitAdmin rabbitAdmin;

	public RabbitTestMessageBus(ConnectionFactory connectionFactory, MultiTypeCodec<Object> codec) {
		super(new RabbitMessageBus(connectionFactory, codec));
		this.rabbitAdmin = new RabbitAdmin(connectionFactory);
	}

	@Override
	public void cleanup() {
		if (!queues.isEmpty()) {
			for (String queue : queues) {
				rabbitAdmin.deleteQueue(queue);
			}
		}
		if (!topics.isEmpty()) {
			for (String exchange : topics) {
				rabbitAdmin.deleteExchange(exchange);
			}
		}
	}
}
