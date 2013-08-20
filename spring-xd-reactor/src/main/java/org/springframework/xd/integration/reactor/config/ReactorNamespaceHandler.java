package org.springframework.xd.integration.reactor.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * @author Jon Brisbin
 */
public class ReactorNamespaceHandler extends AbstractIntegrationNamespaceHandler {
	@Override
	public void init() {
		registerBeanDefinitionParser("syslog-inbound-channel-adapter", new SyslogInboundChannelAdapterParser());
		registerBeanDefinitionParser("channel", new ReactorSubscribableChannelParser());
	}
}
