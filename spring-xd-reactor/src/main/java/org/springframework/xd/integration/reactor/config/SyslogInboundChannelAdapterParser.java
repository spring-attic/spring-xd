package org.springframework.xd.integration.reactor.config;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;
import org.springframework.xd.integration.reactor.syslog.SyslogInboundChannelAdapter;
import org.w3c.dom.Element;

/**
 * @author Jon Brisbin
 */
public class SyslogInboundChannelAdapterParser extends AbstractChannelAdapterParser {
	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		BeanDefinitionBuilder builder = ReactorNamespaceUtils.createBeanDefitionBuilder(SyslogInboundChannelAdapter.class, element);

		String s = element.getAttribute("port");
		int port = 5140;
		if (StringUtils.hasText(s)) {
			port = Integer.parseInt(s);
		}
		builder.addConstructorArgValue(port);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "channel", "outputChannel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel", "errorChannel");

		return builder.getBeanDefinition();
	}
}
