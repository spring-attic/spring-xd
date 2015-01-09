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

package org.springframework.xd.dirt.plugins;

import static org.springframework.xd.module.options.spi.ModulePlaceholders.XD_MODULE_INDEX_KEY;
import static org.springframework.xd.module.options.spi.ModulePlaceholders.XD_MODULE_NAME_KEY;
import static org.springframework.xd.module.options.spi.ModulePlaceholders.XD_MODULE_TYPE_KEY;
import static org.springframework.xd.module.options.spi.ModulePlaceholders.XD_MODULE_COUNT_KEY;
import static org.springframework.xd.module.options.spi.ModulePlaceholders.XD_MODULE_SEQUENCE_KEY;

import java.util.Properties;

import org.springframework.xd.module.RuntimeModuleDeploymentProperties;
import org.springframework.xd.module.core.Module;


/**
 * A plugin that exposes information such as the module name and index to be available inside the environment.
 * 
 * @author Eric Bottard
 * @author Marius Bogoevici
 */
public class ModuleInfoPlugin extends AbstractPlugin {

	@Override
	public boolean supports(Module module) {
		return true;
	}

	@Override
	public void preProcessModule(Module module) {
		Properties props = new Properties();
		props.setProperty(XD_MODULE_NAME_KEY, module.getName());
		props.setProperty(XD_MODULE_INDEX_KEY, String.valueOf(module.getDescriptor().getIndex()));
		props.setProperty(XD_MODULE_TYPE_KEY, module.getType().name());
		props.setProperty(XD_MODULE_COUNT_KEY, String.valueOf(module.getDeploymentProperties().getCount()));
		props.setProperty(XD_MODULE_SEQUENCE_KEY,
				String.valueOf(module.getDeploymentProperties().get(RuntimeModuleDeploymentProperties.SEQUENCE_KEY)));

		module.addProperties(props);
	}

}
