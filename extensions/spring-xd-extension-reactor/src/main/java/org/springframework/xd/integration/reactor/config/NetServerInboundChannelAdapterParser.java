/*
 * Copyright 2013-2014 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.xd.integration.reactor.config;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.util.StringUtils;
import org.springframework.xd.integration.reactor.net.NetServerInboundChannelAdapter;
import org.w3c.dom.Element;

import static org.springframework.integration.config.xml.IntegrationNamespaceUtils.setReferenceIfAttributeDefined;
import static org.springframework.integration.config.xml.IntegrationNamespaceUtils.setValueIfAttributeDefined;

/**
 * @author Jon Brisbin
 */
public class NetServerInboundChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		BeanDefinitionBuilder builder = ReactorNamespaceUtils.createBeanDefinitionBuilder(
				NetServerInboundChannelAdapter.class,
				element
		);

		setValueIfAttributeDefined(builder, element, "dispatcher");
		setValueIfAttributeDefined(builder, element, "host");
		setValueIfAttributeDefined(builder, element, "port");
		setValueIfAttributeDefined(builder, element, "codec");
		setValueIfAttributeDefined(builder, element, "framing");
		String lenFldType = element.getAttribute("length-field-type");
		if(StringUtils.hasText(lenFldType)) {
			int lenFldLen;
			if("short".equals(lenFldType)) {
				lenFldLen = 2;
			} else if("int".equals(lenFldType)) {
				lenFldLen = 4;
			} else if("long".equals(lenFldType)) {
				lenFldLen = 8;
			} else {
				lenFldLen = 4;
			}
			builder.addPropertyValue("lengthFieldLength", lenFldLen);
		}
		setValueIfAttributeDefined(builder, element, "transport");

		setReferenceIfAttributeDefined(builder, element, "channel", "outputChannel");
		setReferenceIfAttributeDefined(builder, element, "error-channel", "errorChannel");

		return builder.getBeanDefinition();
	}

}
