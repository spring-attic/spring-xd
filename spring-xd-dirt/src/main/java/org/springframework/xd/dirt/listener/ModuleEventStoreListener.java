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

import org.springframework.context.ApplicationListener;
import org.springframework.xd.dirt.event.AbstractModuleEvent;
import org.springframework.xd.dirt.event.ModuleDeployedEvent;
import org.springframework.xd.dirt.event.ModuleUndeployedEvent;
import org.springframework.xd.dirt.module.store.RuntimeContainerModuleInfoRepository;
import org.springframework.xd.dirt.module.store.RuntimeModuleInfoEntity;
import org.springframework.xd.dirt.module.store.RuntimeModuleInfoRepository;
import org.springframework.xd.module.core.Module;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Module event listener that stores the module info.
 * 
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
public class ModuleEventStoreListener implements ApplicationListener<AbstractModuleEvent> {

	private final Log logger = LogFactory.getLog(getClass());

	private final RuntimeModuleInfoRepository modulesRepository;

	private final RuntimeContainerModuleInfoRepository modulesPerContainerRepository;

	private final ObjectMapper mapper = new ObjectMapper();


	public ModuleEventStoreListener(RuntimeModuleInfoRepository modulesRepository,
			RuntimeContainerModuleInfoRepository modulesPerContainerRepository) {
		this.modulesRepository = modulesRepository;
		this.modulesPerContainerRepository = modulesPerContainerRepository;
	}

	@Override
	public void onApplicationEvent(AbstractModuleEvent event) {
		Module module = event.getSource();
		Map<String, String> attributes = event.getAttributes();

		try {
			String moduleProperties = this.mapper.writeValueAsString(module.getProperties());
			RuntimeModuleInfoEntity entity = new RuntimeModuleInfoEntity(event.getContainerId(),
					attributes.get("group"),
					attributes.get("index"), moduleProperties);
			if (event instanceof ModuleDeployedEvent) {
				modulesRepository.save(entity);
				modulesPerContainerRepository.save(entity);
			}
			else if (event instanceof ModuleUndeployedEvent) {
				modulesRepository.delete(entity);
				modulesPerContainerRepository.delete(entity);
			}
		}
		catch (JsonProcessingException exception) {
			if (logger.isWarnEnabled()) {
				logger.warn("failed to generate JSON for module properties", exception);
			}
		}
	}
}
