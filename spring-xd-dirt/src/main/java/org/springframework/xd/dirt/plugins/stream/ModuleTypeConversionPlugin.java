/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.plugins.stream;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.CollectionUtils;
import org.springframework.xd.dirt.integration.bus.converter.AbstractFromMessageConverter;
import org.springframework.xd.dirt.integration.bus.converter.CompositeMessageConverterFactory;
import org.springframework.xd.dirt.plugins.AbstractPlugin;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.Plugin;


/**
 * A {@link Plugin} for processing module message conversion parameters (inputType and outputType). Accepts a list of
 * {@link AbstractFromMessageConverter}s which are always available along with an optional list of custom converters
 * which may be provided by end users.
 * 
 * @author David Turanski
 * @since 1.0
 */
public class ModuleTypeConversionPlugin extends AbstractPlugin {

	private final static Log logger = LogFactory.getLog(ModuleTypeConversionPlugin.class);

	private final CompositeMessageConverterFactory converterFactory;

	private final ModuleTypeConversionSupport moduleTypeConversionSupport;

	/**
	 * @param converters a list of default converters
	 * @param customConverters a list of custom converters to extend the default converters
	 */
	public ModuleTypeConversionPlugin(Collection<AbstractFromMessageConverter> converters,
			Collection<AbstractFromMessageConverter> customConverters) {
		if (!CollectionUtils.isEmpty(customConverters)) {
			converters.addAll(customConverters);
		}
		this.converterFactory = new CompositeMessageConverterFactory(converters);
		this.moduleTypeConversionSupport = new ModuleTypeConversionSupport(this.converterFactory);
	}

	/**
	 * Get the underlying {@link org.springframework.xd.dirt.plugins.stream.ModuleTypeConversionSupport} which could be
	 * further used by any other plugin that requires to apply module type conversion explicitly.
	 * See {@link org.springframework.xd.dirt.plugins.spark.streaming.SparkStreamingMessageConverterSupport}
	 *
	 * @return return the underlying module type-conversion support object
	 */
	public ModuleTypeConversionSupport getModuleTypeConversionSupport() {
		return this.moduleTypeConversionSupport;
	}

	@Override
	public void postProcessModule(Module module) {
		if (module.getType() == ModuleType.source || module.getType() == ModuleType.processor) {
			moduleTypeConversionSupport.configureModuleMessageConverters(module, false);
		}
		if (module.getType() == ModuleType.sink || module.getType() == ModuleType.processor) {
			moduleTypeConversionSupport.configureModuleMessageConverters(module, true);
		}
	}

	@Override
	public boolean supports(Module module) {
		return module.shouldBind();
	}

}
