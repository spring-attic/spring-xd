/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.integration.flow.config.xml;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedProperties;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.flow.config.FlowBeanFactoryPostProcessor;
import org.springframework.integration.flow.config.FlowFactoryBean;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * 
 * @author David Turanski
 * @since 3.0
 * 
 */
public class FlowParser implements BeanDefinitionParser {

	private static final String SPRINGFRAMEWORK_P_NAMESPACE_URI = "http://www.springframework.org/schema/p";

	public BeanDefinition parse(Element element, ParserContext parserContext) {

		registerPostProcessorIfFirstInstance(parserContext);

		Element props = DomUtils.getChildElementByTagName(element, "props");

		if (element.hasAttribute("properties") && props != null) {
			parserContext.getReaderContext().error(
					"Element cannot have both 'properties' attribute and inner 'props' element", element);
		}

		BeanDefinitionBuilder flowBuilder = BeanDefinitionBuilder.genericBeanDefinition(FlowFactoryBean.class);
		String id = element.getAttribute("id");
		flowBuilder.addPropertyValue("id", id);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(flowBuilder, element, "properties");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(flowBuilder, element, "module-name");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(flowBuilder, element, "active-profiles");

		if (props != null) {
			flowBuilder.addPropertyValue("properties", parserContext.getDelegate().parsePropsElement(props));
		}

		NamedNodeMap attributes = element.getAttributes();
		ManagedProperties inlineProperties = new ManagedProperties();
		for (int i = 0; i < attributes.getLength(); i++) {
			Attr attribute = (Attr) attributes.item(i);
			if (SPRINGFRAMEWORK_P_NAMESPACE_URI.equals(attribute.getNamespaceURI())) {
				if (element.hasAttribute("properties") || props != null) {
					parserContext
							.getReaderContext()
							.error("Element cannot contain inline properties and a 'properties' attribute or inner 'props' element.",
									element);
				}
				inlineProperties.put(attribute.getLocalName(), attribute.getValue());
			}
		}
		if (!CollectionUtils.isEmpty(inlineProperties)) {
			flowBuilder.addPropertyValue("properties", inlineProperties);
		}

		if (StringUtils.hasText(element.getAttribute("additional-component-locations"))) {
			PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
			ManagedList<Resource> additionalComponentLocations = new ManagedList<Resource>();
			String[] resources = StringUtils.commaDelimitedListToStringArray(element
					.getAttribute("additional-component-locations"));
			for (String resource : resources) {
				try {
					additionalComponentLocations.addAll(Arrays.asList(resolver.getResources(resource)));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			flowBuilder.addPropertyValue("additionalComponentLocations", additionalComponentLocations);
		}

		BeanDefinition beanDefinition = flowBuilder.getBeanDefinition();

		parserContext.getRegistry().registerBeanDefinition(id, beanDefinition);
		return beanDefinition;
	}

	/**
	 * @param parserContext
	 */
	private void registerPostProcessorIfFirstInstance(ParserContext parserContext) {
		if (!parserContext.getRegistry().containsBeanDefinition(FlowBeanFactoryPostProcessor.class.getName())) {
			BeanDefinitionBuilder builder =BeanDefinitionBuilder.genericBeanDefinition(FlowBeanFactoryPostProcessor.class);
			parserContext.getRegistry().registerBeanDefinition(FlowBeanFactoryPostProcessor.class.getName(), builder.getBeanDefinition());
		}
	}
}
