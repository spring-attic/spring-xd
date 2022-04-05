/*
 * Copyright 2013-2014 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.xd.integration.reactor.syslog.SyslogInboundChannelAdapterConfiguration;
import org.springframework.xd.integration.reactor.syslog.SyslogInboundChannelAdapter;
import org.w3c.dom.Element;
import reactor.io.codec.DelimitedCodec;
import reactor.io.codec.StandardCodecs;

/**
 * @author Jon Brisbin
 * @author Stephane Maldini
 */
public class SyslogInboundChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {

		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(SyslogInboundChannelAdapterConfiguration.class);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "host", "host");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "port", "port");

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "channel", "outputChannel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel", "errorChannel");

		return builder.getBeanDefinition();
	}


}
