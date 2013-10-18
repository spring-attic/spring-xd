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

package org.springframework.xd.module.config;

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.xd.module.options.ModuleOption;
import org.springframework.xd.module.options.ModuleOptions;


/**
 * Parses the top-level {@code <xd:options ...>} XML element, emitting a {@link ModuleOptions}.
 * 
 * @author Eric Bottard
 */
public class ModuleOptionsBeanDefinitionParser implements BeanDefinitionParser {

	/**
	 * Name of the XML element representing an individual option.
	 */
	private static final String OPTION = "option";

	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ModuleOptions.class);

		registerModuleOptions(element, parserContext, builder);

		builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
		BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());

		return builder.getBeanDefinition();
	}

	private void registerModuleOptions(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		List<Element> options = DomUtils.getChildElementsByTagName(element, OPTION);

		ManagedList<BeanMetadataElement> optionsList = new ManagedList<BeanMetadataElement>();
		for (Element optionElement : options) {
			BeanDefinitionBuilder optionBuilder = BeanDefinitionBuilder.genericBeanDefinition(ModuleOption.class);
			optionBuilder.addConstructorArgValue(optionElement.getAttribute("name"));
			optionBuilder.addPropertyValue("description", optionElement.getAttribute("description"));
			optionBuilder.addPropertyValue("type", shortToFQDN(optionElement.getAttribute("type")));

			String defaultValue = element.getAttribute("default-value");
			String defaultExpression = element.getAttribute("default-expression");
			if (StringUtils.hasText(defaultValue) && StringUtils.hasText(defaultExpression)) {
				parserContext.getReaderContext().error(
						"Only one of 'default-value' or 'default-expression' is allowed",
						element.getAttributeNode("default-value"));
			}
			optionBuilder.addPropertyValue("defaultValue", defaultValue);
			optionBuilder.addPropertyValue("defaultExpression", defaultExpression);

			optionsList.add(optionBuilder.getBeanDefinition());
		}

		builder.addPropertyValue("options", optionsList);

	}

	private String shortToFQDN(String type) {
		return type;
	}

}
