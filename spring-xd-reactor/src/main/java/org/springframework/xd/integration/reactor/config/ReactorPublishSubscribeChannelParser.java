package org.springframework.xd.integration.reactor.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelParser;
import org.springframework.util.StringUtils;
import org.springframework.xd.integration.reactor.channel.ReactorPublishSubscribeChannel;
import org.springframework.xd.integration.reactor.dispatcher.ReactorProcessorMessageDispatcher;
import org.w3c.dom.Element;

/**
 * @author Jon Brisbin
 */
public class ReactorPublishSubscribeChannelParser extends AbstractChannelParser {
	@Override
	protected BeanDefinitionBuilder buildBeanDefinition(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ReactorPublishSubscribeChannel.class);

		String s = element.getAttribute("dispatcher");
		if (!StringUtils.hasText(s)) {
			builder.addConstructorArgValue(new ReactorProcessorMessageDispatcher());
		} else {
			builder.addConstructorArgReference(s);
		}

		return builder;
	}
}
