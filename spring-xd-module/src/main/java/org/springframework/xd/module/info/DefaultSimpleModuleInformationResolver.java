/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.module.info;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.validation.BindException;
import org.springframework.validation.beanvalidation.CustomValidatorBean;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.SimpleModuleDefinition;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;
import org.springframework.xd.module.options.ModuleUtils;

/**
 * Default implementation of {@link org.springframework.xd.module.info.ModuleInformationResolver} that supports
 * {@link org.springframework.xd.module.SimpleModuleDefinition}s and that reads Strings from
 * the module properties companion file.
 *
 * @author Eric Bottard
 */
public class DefaultSimpleModuleInformationResolver implements ModuleInformationResolver {

	private CustomValidatorBean validator = new CustomValidatorBean();

	public DefaultSimpleModuleInformationResolver() {
		validator.afterPropertiesSet();
	}

	@Override
	public ModuleInformation resolve(ModuleDefinition definition) {
		if (!(definition instanceof SimpleModuleDefinition)) {
			return null;
		}
		SimpleModuleDefinition simpleModuleDefinition = (SimpleModuleDefinition) definition;

		Properties props = ModuleUtils.loadModuleProperties(simpleModuleDefinition);
		if (props == null) {
			return null;
		}
		else {
			try {
				ModuleInformation result = new ModuleInformation();
				PropertiesConfigurationFactory<ModuleInformation> factory = new PropertiesConfigurationFactory<>(result);
				factory.setProperties(props);
				factory.setTargetName("info");
				factory.setValidator(validator);
				factory.bindPropertiesToTarget();
				return result;
			}
			catch (BindException e) {
				throw new RuntimeException("Exception occurred trying to set values from properties file for " + definition + "\n" + e.getMessage(), e);
			}
		}
	}
}
