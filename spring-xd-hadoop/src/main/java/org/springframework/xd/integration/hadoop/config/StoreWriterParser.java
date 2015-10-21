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

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;
import org.springframework.xd.integration.hadoop.IntegrationHadoopSystemConstants;

/**
 * Parser for the 'storage' element.
 * 
 * @author Janne Valkealahti
 * 
 */
public class StoreWriterParser extends AbstractSimpleBeanDefinitionParser {

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "configuration");
		IntegrationHadoopNamespaceUtils.setPathReference(element, parserContext, builder,
				"base-path", IntegrationHadoopSystemConstants.DEFAULT_DATA_PATH);
		IntegrationHadoopNamespaceUtils.setCodecInfoReference(element, parserContext, builder,
				"codec");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "in-use-suffix");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "in-use-prefix");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "idle-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "close-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "flush-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "enable-sync");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "overwrite");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "naming-strategy");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "rollover-strategy");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "partition-expression");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "file-open-attempts");
	}

	@Override
	protected Class<?> getBeanClass(Element element) {
		return StoreWriterFactoryBean.class;
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {
		String name = super.resolveId(element, definition, parserContext);
		if (!StringUtils.hasText(name)) {
			name = IntegrationHadoopSystemConstants.DEFAULT_ID_STORE_WRITER;
		}
		return name;
	}

}
