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
import org.codehaus.jackson.map.ObjectMapper;

import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.xd.dirt.event.AbstractModuleEvent;
import org.springframework.xd.dirt.event.ModuleDeployedEvent;
import org.springframework.xd.dirt.event.ModuleUndeployedEvent;
import org.springframework.xd.module.Module;

/**
 * @author Mark Fisher
 */
public class RedisModuleEventListener implements ApplicationListener<AbstractModuleEvent> {

	private final Log logger = LogFactory.getLog(getClass());

	private final StringRedisTemplate redisTemplate = new StringRedisTemplate();

	private final ObjectMapper mapper = new ObjectMapper();


	public RedisModuleEventListener(RedisConnectionFactory redisConnectionFactory) {
		this.redisTemplate.setConnectionFactory(redisConnectionFactory);
	}


	@Override
	public void onApplicationEvent(AbstractModuleEvent event) {
		Module module = event.getSource();
		Map<String, String> attributes = event.getAttributes();
		String key = "modules:" + attributes.get("group");
		String hashKey = module.getName() + "." + attributes.get("index");
		try {
			String properties = this.mapper.writeValueAsString(module.getProperties());
			if (event instanceof ModuleDeployedEvent) {
				this.redisTemplate.boundHashOps(key).put(hashKey, properties);
			}
			else if (event instanceof ModuleUndeployedEvent) {
				this.redisTemplate.boundHashOps(key).delete(hashKey);
			}
		}
		catch (Exception e) {
			if (logger.isWarnEnabled()) {
				logger.warn("failed to generate JSON for module properties", e);
			}
		}
	}

}
