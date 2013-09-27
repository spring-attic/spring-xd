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

package org.springframework.xd.dirt.listener;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.xd.dirt.container.ContainerStartedEvent;
import org.springframework.xd.dirt.container.ContainerStoppedEvent;
import org.springframework.xd.dirt.container.XDContainer;

/**
 * @author Mark Fisher
 */
public class RabbitContainerEventListener extends AbstractContainerEventListener {

	private static final String CONTAINER_EVENT_EXCHANGE = "xd.events.containers";

	private final RabbitTemplate rabbitTemplate = new RabbitTemplate();

	public RabbitContainerEventListener(ConnectionFactory connectionFactory) {
		this.rabbitTemplate.setConnectionFactory(connectionFactory);
		this.rabbitTemplate.afterPropertiesSet();
	}

	@Override
	protected void onContainerStartedEvent(ContainerStartedEvent event) {
		final XDContainer container = event.getSource();
		this.rabbitTemplate.convertAndSend(CONTAINER_EVENT_EXCHANGE, "",
				"container started [" + container.getId() + ":" + container.getJvmName() + "]");
	}

	@Override
	protected void onContainerStoppedEvent(ContainerStoppedEvent event) {
		XDContainer container = event.getSource();
		this.rabbitTemplate.convertAndSend(CONTAINER_EVENT_EXCHANGE, "",
				"container stopped [" + container.getId() + ":" + container.getJvmName() + "]");
	}

}
