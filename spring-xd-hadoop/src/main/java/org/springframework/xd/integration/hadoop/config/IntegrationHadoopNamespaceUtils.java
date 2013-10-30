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

package org.springframework.xd.integration.hadoop.config;

import org.apache.hadoop.fs.Path;
import org.w3c.dom.Element;

import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.data.hadoop.store.codec.Codecs;
import org.springframework.util.StringUtils;

/**
 * Utility methods for namespace.
 * 
 * @author Janne Valkealahti
 * 
 */
public abstract class IntegrationHadoopNamespaceUtils {

	/**
	 * Adds the path constructor arg reference. Creates new hdfs {@code Path} as a spring bean order to play nice with
	 * property placeholder and expressions.
	 * 
	 * @param element the element
	 * @param parserContext the parser context
	 * @param builder the builder
	 * @param attributeName the attribute name
	 * @param defaultPath the default path
	 */
	public static void addPathConstructorArgReference(Element element, ParserContext parserContext,
			BeanDefinitionBuilder builder, String attributeName, String defaultPath) {
		String attribute = element.getAttribute(attributeName);
		if (!StringUtils.hasText(attribute)) {
			attribute = defaultPath;
		}
		BeanDefinitionBuilder pathBuilder = BeanDefinitionBuilder.genericBeanDefinition(Path.class);
		pathBuilder.addConstructorArgValue(attribute);
		AbstractBeanDefinition beanDef = pathBuilder.getBeanDefinition();
		String beanName = BeanDefinitionReaderUtils.generateBeanName(beanDef, parserContext.getRegistry());
		parserContext.registerBeanComponent(new BeanComponentDefinition(beanDef, beanName));
		builder.addConstructorArgReference(beanName);
	}

	/**
	 * Adds the codec info constructor arg reference. Creates new {@code CodecInfo} bean via
	 * {@code MethodInvokingFactoryBean} by calling static {@code Codecs#getCodecInfo(String)} order to play nice with
	 * property placeholder and expressions.
	 * 
	 * @param element the element
	 * @param parserContext the parser context
	 * @param builder the builder
	 * @param attributeName the attribute name
	 */
	public static void addCodecInfoConstructorArgReference(Element element, ParserContext parserContext,
			BeanDefinitionBuilder builder, String attributeName) {
		BeanDefinitionBuilder codecBuilder = BeanDefinitionBuilder.genericBeanDefinition(MethodInvokingFactoryBean.class);

		codecBuilder.addPropertyValue("targetClass", Codecs.class);
		codecBuilder.addPropertyValue("targetMethod", "getCodecInfo");
		codecBuilder.addPropertyValue("arguments", new String[] { element.getAttribute(attributeName) });

		AbstractBeanDefinition beanDef = codecBuilder.getBeanDefinition();
		String beanName = BeanDefinitionReaderUtils.generateBeanName(beanDef, parserContext.getRegistry());
		parserContext.registerBeanComponent(new BeanComponentDefinition(beanDef, beanName));
		builder.addConstructorArgReference(beanName);
	}

}
