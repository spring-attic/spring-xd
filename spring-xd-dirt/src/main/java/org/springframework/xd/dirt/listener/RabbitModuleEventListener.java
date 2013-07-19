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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationListener;
import org.springframework.xd.dirt.event.AbstractModuleEvent;
import org.springframework.xd.dirt.event.ModuleDeployedEvent;
import org.springframework.xd.dirt.event.ModuleUndeployedEvent;
import org.springframework.xd.module.Module;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Mark Fisher
 */
public class RabbitModuleEventListener implements ApplicationListener<AbstractModuleEvent> {

	private static final String MODULE_EVENT_EXCHANGE = "xd.events.modules";

	private final Log logger = LogFactory.getLog(getClass());

	private final RabbitTemplate rabbitTemplate = new RabbitTemplate();

	private final ObjectMapper mapper = new ObjectMapper();


	public RabbitModuleEventListener(ConnectionFactory connectionFactory) {
		this.rabbitTemplate.setConnectionFactory(connectionFactory);
		this.rabbitTemplate.afterPropertiesSet();
	}


	@Override
	public void onApplicationEvent(AbstractModuleEvent event) {
		Module module = event.getSource();
		Map<String, String> attributes = event.getAttributes();
		String hashKey = module.getName() + "." + attributes.get("index");
		try {
			String properties = this.mapper.writeValueAsString(module.getProperties());
			if (event instanceof ModuleDeployedEvent) {
				this.rabbitTemplate.convertAndSend(MODULE_EVENT_EXCHANGE, "",
						"module '" + hashKey + "' deployed with properties: " + properties);
			}
			else if (event instanceof ModuleUndeployedEvent) {
				this.rabbitTemplate.convertAndSend(MODULE_EVENT_EXCHANGE, "",
						"module '" + hashKey + "' undeployed");
			}
		}
		catch (Exception e) {
			if (logger.isWarnEnabled()) {
				logger.warn("failed to generate JSON for module properties", e);
			}
		}
	}

}
