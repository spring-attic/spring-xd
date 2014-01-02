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

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.x.bus.converter.ByteArrayToStringConverter;
import org.springframework.integration.x.bus.converter.ContentTypeHeaderInterceptor;
import org.springframework.integration.x.bus.converter.DefaultContentTypeAwareConverterRegistry;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.plugins.ModuleConfigurationException;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.Plugin;
import org.springframework.xd.module.core.PluginAdapter;
import org.springframework.xd.module.core.SimpleModule;


/**
 * A {@link Plugin} for processing module message conversion parameters (inputType and outputType)
 * 
 * @author David Turanski
 * @since 1.0
 */
public class ModuleTypeConversionPlugin extends PluginAdapter {

	private final static Log logger = LogFactory.getLog(ModuleTypeConversionPlugin.class);

	private final DefaultContentTypeAwareConverterRegistry converterRegistry = new DefaultContentTypeAwareConverterRegistry();

	@Override
	public void postProcessModule(Module module) {
		String outputType = null;
		String inputType = null;
		if (module.getType() == ModuleType.source || module.getType() == ModuleType.processor) {
			outputType = module.getProperties().getProperty("outputType");
		}
		if (module.getType() == ModuleType.sink || module.getType() == ModuleType.processor) {
			inputType = module.getProperties().getProperty("inputType");
		}
		DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
		if (outputType != null) {
			configureModuleConverters(outputType, module, conversionService, false);
		}
		if (inputType != null) {
			configureModuleConverters(inputType, module, conversionService, true);
		}

		registerConversionService(module, conversionService);
	}

	private void registerConversionService(Module module, ConversionService conversionService) {
		if (module instanceof SimpleModule) {
			SimpleModule sm = (SimpleModule) module;
			ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) sm.getApplicationContext();
			applicationContext.getBeanFactory().registerSingleton("conversionService", conversionService);
		}
	}

	private void configureModuleConverters(String contentTypeString, Module module,
			ConfigurableConversionService conversionService, boolean isInput) {
		if (logger.isDebugEnabled()) {
			logger.debug("module " + (isInput ? "input" : "output") + "Type is " + contentTypeString);
		}
		SimpleModule sm = (SimpleModule) module;
		try {
			MediaType contentType = resolveContentType(contentTypeString, module);
			AbstractMessageChannel channel = null;
			if (isInput) {
				channel = module.getComponent("input", AbstractMessageChannel.class);
			}
			else {
				channel = module.getComponent("output", AbstractMessageChannel.class);
			}
			Map<Class<?>, Converter<?, ?>> converters = converterRegistry.getConverters(contentType);
			if (CollectionUtils.isEmpty(converters)) {
				throw new ModuleConfigurationException("No message converter is registered for " + contentTypeString +
						"(" +
						module.getName() + " --" + (isInput ? "input" : "output") + "Type=" + contentTypeString
						+ ")");
			}

			Class<?> dataType = converterRegistry.getJavaTypeForContentType(contentType,
					sm.getApplicationContext().getClassLoader());
			if (dataType == null) {
				throw new ModuleConfigurationException("Content type is not supported for " +
						module.getName() + " --" + (isInput ? "input" : "output") + "Type=" + contentTypeString);
			}
			else {
				channel.addInterceptor(new ContentTypeHeaderInterceptor(contentType));
				channel.setDatatypes(dataType);

				for (Entry<Class<?>, Converter<?, ?>> entry : converters.entrySet()) {
					if (logger.isDebugEnabled()) {
						logger.debug("registering converter sourceType [" + entry.getKey().getName() +
								"] targetType [" + dataType.getName() + "] converter ["
								+ entry.getValue().getClass().getName() + "]");

					}
					Converter<?, ?> converter = entry.getValue();
					// special case to handle charset parameter
					if (converter instanceof ByteArrayToStringConverter) {
						if (MediaType.TEXT_PLAIN.includes(contentType)) {
							String charsetName = contentType.getParameter("charset");
							if (StringUtils.hasText(charsetName)) {
								converter = new ByteArrayToStringConverter(charsetName);
							}
						}
					}
					conversionService.addConverter(entry.getKey(), dataType, converter);
				}
				channel.setConversionService(conversionService);
			}

		}
		catch (Throwable t) {
			throw new ModuleConfigurationException(t.getMessage(), t);
		}
	}

	private MediaType resolveContentType(String type, Module module) throws ClassNotFoundException, LinkageError {
		if (!type.contains("/")) {
			Class<?> javaType = resolveJavaType(type, module);
			return MediaType.valueOf("application/x-java-object;type=" + javaType.getName());
		}
		return MediaType.valueOf(type);
	}

	private Class<?> resolveJavaType(String type, Module module) throws ClassNotFoundException, LinkageError {
		SimpleModule sm = (SimpleModule) module;
		return ClassUtils.forName(type, sm.getApplicationContext().getClassLoader());
	}

}
