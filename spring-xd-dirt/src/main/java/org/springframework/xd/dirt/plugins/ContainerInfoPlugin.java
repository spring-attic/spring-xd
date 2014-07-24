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

package org.springframework.xd.dirt.plugins;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.xd.dirt.cluster.ContainerAttributes;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.options.spi.ModulePlaceholders;


/**
 * A plugin that exposes information about the container to the Environment.
 * 
 * @author Eric Bottard
 */
public class ContainerInfoPlugin extends AbstractPlugin {

	private final ContainerAttributes attributes;

	@Autowired
	public ContainerInfoPlugin(ContainerAttributes attributes) {
		this.attributes = attributes;
	}

	@Override
	public boolean supports(Module module) {
		return true;
	}

	@Override
	public void preProcessModule(Module module) {
		Properties props = new Properties();
		for (String key : attributes.keySet()) {
			props.setProperty(ModulePlaceholders.XD_CONTAINER_KEY_PREFIX + key, attributes.get(key));
		}

		module.addProperties(props);
	}

}
