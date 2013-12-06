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
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.data.hadoop.store.output.TextFileWriter;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;
import org.springframework.xd.integration.hadoop.IntegrationHadoopSystemConstants;

/**
 * Parser for the 'storage' element.
 * 
 * @author Janne Valkealahti
 * 
 */
public class StoreWriterParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(TextFileWriter.class);

		builder.addConstructorArgReference(element.getAttribute("configuration"));
		IntegrationHadoopNamespaceUtils.addPathConstructorArgReference(element, parserContext, builder, "base-path",
				IntegrationHadoopSystemConstants.DEFAULT_DATA_PATH);
		IntegrationHadoopNamespaceUtils.addCodecInfoConstructorArgReference(element, parserContext, builder, "codec");

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "in-use-suffix", "inWritingSuffix");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "in-use-prefix", "inWritingPrefix");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "idle-timeout");

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "naming-strategy",
				"fileNamingStrategy");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "rollover-strategy",
				"rolloverStrategy");

		return builder.getBeanDefinition();
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
