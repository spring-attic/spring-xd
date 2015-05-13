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
package org.springframework.xd.dirt.plugins.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.integration.bus.converter.CompositeMessageConverterFactory;
import org.springframework.xd.dirt.integration.bus.converter.ConversionException;
import org.springframework.xd.dirt.integration.bus.converter.MessageConverterUtils;
import org.springframework.xd.dirt.plugins.ModuleConfigurationException;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.SimpleModule;

/**
 * Support class that configures the message converters for the module input/output channel.
 *
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 */
public class ModuleTypeConversionSupport {

	private final static Logger logger = LoggerFactory.getLogger(ModuleTypeConversionSupport.class);

	private final CompositeMessageConverterFactory converterFactory;

	private static final String INPUT_TYPE = "inputType";

	private static final String OUTPUT_TYPE = "outputType";

	public ModuleTypeConversionSupport(CompositeMessageConverterFactory converterFactory) {
		this.converterFactory = converterFactory;
	}

	/**
	 * Return {@link org.springframework.util.MimeType} for the module's input channel.
	 *
	 * @param module the underlying module
	 * @return MimeType for the module's input channel, null if not set
	 */
	public static MimeType getInputMimeType(Module module) {
		String contentTypeString = module.getProperties().getProperty(INPUT_TYPE);
		return getMimeType(contentTypeString, module);
	}

	/**
	 * Return {@link org.springframework.util.MimeType} for the module's output channel.
	 *
	 * @param module the underlying module
	 * @return MimeType for the module's output channel, null if not set
	 */
	public static MimeType getOutputMimeType(Module module) {
		String contentTypeString = module.getProperties().getProperty(OUTPUT_TYPE);
		return getMimeType(contentTypeString, module);
	}

	/**
	 * Return {@link org.springframework.util.MimeType} for the given content type string.
	 *
	 * @param contentTypeString the content type string
	 * @param module the underlying module
	 * @return the MimeType for the content type string
	 */
	private static MimeType getMimeType(String contentTypeString, Module module) {
		MimeType mimeType = null;
		if (StringUtils.hasText(contentTypeString)) {
			try {
				mimeType = resolveContentType(contentTypeString, module);
			}
			catch (ClassNotFoundException cfe) {
				throw new IllegalArgumentException("Could not find the class required for " + contentTypeString, cfe);
			}
		}
		return mimeType;
	}


	/**
	 * Configure message converters for the given module's input channel.
	 *
	 * @param module the underlying module
	 */
	protected void configureModuleInputChannelMessageConverters(Module module) {
		MimeType contentType = getInputMimeType(module);
		if (contentType != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("configuring message converters for module " + module.getName() + "'s input channel.  " +
						"Type is " + contentType);
			}
			Assert.isTrue((module instanceof SimpleModule), "Module should be an instance of " +
					SimpleModule.class.getName());
			SimpleModule sm = (SimpleModule) module;
			try {
				AbstractMessageChannel channel = getChannel(module, true);
				configureMessageConverters(channel, contentType, sm.getApplicationContext().getClassLoader());
			}
			catch (Exception e) {
				throw new ModuleConfigurationException(e.getMessage(), e);
			}
		}
	}

	/**
	 * Configure message converters for the given module's output channel.
	 *
	 * @param module the underlying module
	 */
	protected void configureModuleOutputChannelMessageConverters(Module module) {
		MimeType contentType = getOutputMimeType(module);
		if (contentType != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("configuring message converters for module " + module.getName() + "'s output channel.  " +
						"Type is " + contentType);
			}
			SimpleModule sm = (SimpleModule) module;
			try {
				AbstractMessageChannel channel = getChannel(module, false);
				configureMessageConverters(channel, contentType, sm.getApplicationContext().getClassLoader());
			}
			catch (Exception e) {
				throw new ModuleConfigurationException(e.getMessage(), e);
			}
		}
	}

	/**
	 * Configure the message converters for the given message channel and content type.
	 * Detect the data types from the provided class loader.
	 *
	 * @param channel the message channel to configure the message converters
	 * @param contentType the content type to use
	 * @param classLoader the classloader to search the dataType(s) classes
	 */
	public void configureMessageConverters(AbstractMessageChannel channel, MimeType contentType,
			ClassLoader classLoader) {
		CompositeMessageConverter converters = null;
		try {
			converters = converterFactory.newInstance(contentType);
		}
		catch (ConversionException e) {
			throw new ModuleConfigurationException(e.getMessage() +
					"(" +
					channel.getComponentName() + "Type=" + contentType + ")");
		}

		Class<?> dataType = MessageConverterUtils.getJavaTypeForContentType(contentType, classLoader);
		if (dataType == null) {
			throw new ModuleConfigurationException("Content type is not supported for " +
					channel.getComponentName() + "Type=" + contentType);
		}
		else {
			channel.setDatatypes(dataType);
			channel.setMessageConverter(converters);
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

	/**
	 * Resolve the {@link org.springframework.util.MimeType} to use for the given content type
	 * specified for the module input/output.
	 *
	 * @param type the content type
	 * @param module the underlying module
	 * @return the MimeType
	 * @throws ClassNotFoundException
	 * @throws LinkageError
	 */
	public static MimeType resolveContentType(String type, Module module) throws ClassNotFoundException, LinkageError {
		if (!type.contains("/")) {
			Class<?> javaType = resolveJavaType(type, module);
			return MessageConverterUtils.javaObjectMimeType(javaType);
		}
		return MimeType.valueOf(type);
	}

	private static Class<?> resolveJavaType(String type, Module module) throws ClassNotFoundException, LinkageError {
		Assert.isTrue((module instanceof SimpleModule), "Module should be an instance of " +
				SimpleModule.class.getName());
		SimpleModule sm = (SimpleModule) module;
		return ClassUtils.forName(type, sm.getApplicationContext().getClassLoader());
	}
}
