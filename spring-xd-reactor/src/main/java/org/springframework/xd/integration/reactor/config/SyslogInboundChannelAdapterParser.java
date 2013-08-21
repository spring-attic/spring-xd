package org.springframework.xd.integration.reactor.config;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.xd.integration.reactor.syslog.SyslogInboundChannelAdapter;
import org.w3c.dom.Element;

/**
 * @author Jon Brisbin
 */
public class SyslogInboundChannelAdapterParser extends AbstractChannelAdapterParser {
	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		BeanDefinitionBuilder builder = ReactorNamespaceUtils.createBeanDefinitionBuilder(SyslogInboundChannelAdapter.class, element);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "host", "host");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "port", "port");

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel", "errorChannel");

		return builder.getBeanDefinition();
	}
}
