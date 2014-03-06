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
