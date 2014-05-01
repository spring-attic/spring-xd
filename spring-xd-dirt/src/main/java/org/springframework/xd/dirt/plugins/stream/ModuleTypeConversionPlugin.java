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

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.xd.dirt.integration.bus.converter.AbstractFromMessageConverter;
import org.springframework.xd.dirt.integration.bus.converter.CompositeMessageConverterFactory;
import org.springframework.xd.dirt.integration.bus.converter.ConversionException;
import org.springframework.xd.dirt.integration.bus.converter.MessageConverterUtils;
import org.springframework.xd.dirt.plugins.AbstractPlugin;
import org.springframework.xd.dirt.plugins.ModuleConfigurationException;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.Plugin;
import org.springframework.xd.module.core.SimpleModule;


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
	}

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
		if (outputType != null) {
			configureModuleConverters(outputType, module, false);
		}
		if (inputType != null) {
			configureModuleConverters(inputType, module, true);
		}
	}

	private void configureModuleConverters(String contentTypeString, Module module, boolean isInput) {
		if (logger.isDebugEnabled()) {
			logger.debug("module " + (isInput ? "input" : "output") + "Type is " + contentTypeString);
		}
		SimpleModule sm = (SimpleModule) module;
		try {
			MimeType contentType = resolveContentType(contentTypeString, module);

			AbstractMessageChannel channel = getChannel(module, isInput);

			CompositeMessageConverter converters = null;
			try {
				converters = converterFactory.newInstance(contentType);
			}
			catch (ConversionException e) {
				throw new ModuleConfigurationException(e.getMessage() +
						"(" +
						module.getName() + " --" + (isInput ? "input" : "output") + "Type=" + contentTypeString
						+ ")");
			}

			Class<?> dataType = MessageConverterUtils.getJavaTypeForContentType(contentType,
					sm.getApplicationContext().getClassLoader());
			if (dataType == null) {
				throw new ModuleConfigurationException("Content type is not supported for " +
						module.getName() + " --" + (isInput ? "input" : "output") + "Type=" + contentTypeString);
			}
			else {
				channel.setDatatypes(dataType);
				channel.setMessageConverter(converters);
			}

		}
		catch (Throwable t) {
			throw new ModuleConfigurationException(t.getMessage(), t);
		}
	}

	// Workaround for when the channel is proxied and can't be cast directly to AbstractMessageChannel
	private AbstractMessageChannel getChannel(Module module, boolean isInput) throws Exception {
		String name = isInput ? "input" : "output";
		Object channel = module.getComponent(name, Object.class);

		if (AopUtils.isJdkDynamicProxy(channel)) {
			return (AbstractMessageChannel) (((Advised) channel).getTargetSource().getTarget());
		}

		return (AbstractMessageChannel) channel;
	}

	private MimeType resolveContentType(String type, Module module) throws ClassNotFoundException, LinkageError {
		if (!type.contains("/")) {
			Class<?> javaType = resolveJavaType(type, module);
			return MessageConverterUtils.javaObjectMimeType(javaType);
		}
		return MimeType.valueOf(type);
	}

	private Class<?> resolveJavaType(String type, Module module) throws ClassNotFoundException, LinkageError {
		SimpleModule sm = (SimpleModule) module;
		return ClassUtils.forName(type, sm.getApplicationContext().getClassLoader());
	}

	@Override
	public boolean supports(Module module) {
		return true;
	}

}
