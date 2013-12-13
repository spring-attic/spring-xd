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
import org.springframework.data.hadoop.store.strategy.rollover.ChainedRolloverStrategy;
import org.springframework.data.hadoop.store.strategy.rollover.NoRolloverStrategy;
import org.springframework.data.hadoop.store.strategy.rollover.SizeRolloverStrategy;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.xd.integration.hadoop.IntegrationHadoopSystemConstants;

/**
 * Parser for the 'rollover-policy' element.
 * 
 * @author Janne Valkealahti
 * 
 */
public class RolloverStrategyParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder;

		ManagedList<RuntimeBeanReference> strategies = new ManagedList<RuntimeBeanReference>();
		List<Element> sizeElements = DomUtils.getChildElementsByTagName(element, "size");

		if (sizeElements.size() == 0) {
			builder = BeanDefinitionBuilder.genericBeanDefinition(NoRolloverStrategy.class);
			return builder.getBeanDefinition();
		}

		builder = BeanDefinitionBuilder.genericBeanDefinition(ChainedRolloverStrategy.class);

		for (Element e : sizeElements) {
			BeanDefinitionBuilder nestedBuilder = BeanDefinitionBuilder.genericBeanDefinition(SizeRolloverStrategy.class);

			String sizeValue = e.getAttribute("size");
			if (StringUtils.hasText(sizeValue)) {
				nestedBuilder.addConstructorArgValue(sizeValue);
			}

			IntegrationNamespaceUtils.setValueIfAttributeDefined(nestedBuilder, e, "order");
			String nestedBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
					nestedBuilder.getBeanDefinition(),
					parserContext.getRegistry());
			strategies.add(new RuntimeBeanReference(nestedBeanName));
		}

		builder.addPropertyValue("strategies", strategies);
		return builder.getBeanDefinition();
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {
		String name = super.resolveId(element, definition, parserContext);
		if (!StringUtils.hasText(name)) {
			name = IntegrationHadoopSystemConstants.DEFAULT_ID_FILE_ROLLOVER_STRATEGY;
		}
		return name;
	}

}
