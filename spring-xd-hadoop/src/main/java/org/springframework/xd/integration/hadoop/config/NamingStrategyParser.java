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

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.data.hadoop.store.strategy.naming.ChainedFileNamingStrategy;
import org.springframework.data.hadoop.store.strategy.naming.CodecFileNamingStrategy;
import org.springframework.data.hadoop.store.strategy.naming.RollingFileNamingStrategy;
import org.springframework.data.hadoop.store.strategy.naming.StaticFileNamingStrategy;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.xd.integration.hadoop.IntegrationHadoopSystemConstants;

/**
 * Parser for the 'naming-policy' element.
 * 
 * @author Janne Valkealahti
 * 
 */
public class NamingStrategyParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder;

		ManagedList<RuntimeBeanReference> strategies = new ManagedList<RuntimeBeanReference>();
		List<Element> staticElements = DomUtils.getChildElementsByTagName(element, "static");
		List<Element> rollingElements = DomUtils.getChildElementsByTagName(element, "rolling");
		List<Element> renamingElements = DomUtils.getChildElementsByTagName(element, "renaming");
		List<Element> codecElements = DomUtils.getChildElementsByTagName(element, "codec");

		if (staticElements.size() == 0 && rollingElements.size() == 0 && renamingElements.size() == 0
				&& codecElements.size() == 0) {
			builder = BeanDefinitionBuilder.genericBeanDefinition(StaticFileNamingStrategy.class);
			return builder.getBeanDefinition();
		}

		builder = BeanDefinitionBuilder.genericBeanDefinition(ChainedFileNamingStrategy.class);

		for (Element e : staticElements) {
			BeanDefinitionBuilder nestedBuilder = BeanDefinitionBuilder.genericBeanDefinition(StaticFileNamingStrategy.class);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(nestedBuilder, e, "order");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(nestedBuilder, e, "file-name", "fileName");
			String nestedBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
					nestedBuilder.getBeanDefinition(),
					parserContext.getRegistry());
			strategies.add(new RuntimeBeanReference(nestedBeanName));
		}

		for (Element e : rollingElements) {
			BeanDefinitionBuilder nestedBuilder = BeanDefinitionBuilder.genericBeanDefinition(RollingFileNamingStrategy.class);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(nestedBuilder, e, "order");
			String nestedBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
					nestedBuilder.getBeanDefinition(),
					parserContext.getRegistry());
			strategies.add(new RuntimeBeanReference(nestedBeanName));
		}

		for (Element e : codecElements) {
			BeanDefinitionBuilder nestedBuilder = BeanDefinitionBuilder.genericBeanDefinition(CodecFileNamingStrategy.class);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(nestedBuilder, e, "order");
			String nestedBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
					nestedBuilder.getBeanDefinition(),
					parserContext.getRegistry());
			strategies.add(new RuntimeBeanReference(nestedBeanName));
		}

		// for (Element e : renamingElements) {
		// BeanDefinitionBuilder nestedBuilder =
		// BeanDefinitionBuilder.genericBeanDefinition(RenamingFileNamingStrategy.class);
		// IntegrationNamespaceUtils.setValueIfAttributeDefined(nestedBuilder, e, "order");
		// IntegrationNamespaceUtils.setValueIfAttributeDefined(nestedBuilder, e, "prefix");
		// IntegrationNamespaceUtils.setValueIfAttributeDefined(nestedBuilder, e, "suffix");
		// IntegrationNamespaceUtils.setReferenceIfAttributeDefined(nestedBuilder, e, "configuration");
		// String nestedBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
		// nestedBuilder.getBeanDefinition(),
		// parserContext.getRegistry());
		// strategies.add(new RuntimeBeanReference(nestedBeanName));
		// }

		builder.addPropertyValue("strategies", strategies);
		return builder.getBeanDefinition();
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {
		String name = super.resolveId(element, definition, parserContext);
		if (!StringUtils.hasText(name)) {
			name = IntegrationHadoopSystemConstants.DEFAULT_ID_FILE_NAMING_STRATEGY;
		}
		return name;
	}

}
