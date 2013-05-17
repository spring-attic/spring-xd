/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.integration.hadoop.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * Parser for the 'hdfs-outbound-channel-adapter' element.
 *
 * @author Mark Fisher
 */
public class HdfsOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(HdfsWritingMessageHandlerFactoryBean.class);
		String fileSystem = element.getAttribute("file-system");
		if (!StringUtils.hasText(fileSystem)) {
			parserContext.getReaderContext().error("file-system is required", element);
		}
		builder.addConstructorArgReference(fileSystem);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "base-path");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "base-filename");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "file-suffix");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "rollover-threshold-in-bytes");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
		return builder.getBeanDefinition();
	}

}
