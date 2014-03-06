package org.springframework.xd.integration.reactor.config;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import reactor.spring.core.task.RingBufferAsyncTaskExecutor;

/**
 * Namespace parser for creating {@link reactor.spring.core.task.RingBufferAsyncTaskExecutor} instances.
 *
 * @author Jon Brisbin
 */
public class RingBufferTaskExecutorParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		String id = element.getAttribute("id");
		if(!StringUtils.hasText(id)) {
			id = RingBufferAsyncTaskExecutor.class.getSimpleName();
		}

		BeanDefinitionBuilder builder = ReactorNamespaceUtils.createBeanDefinitionBuilder(
				RingBufferAsyncTaskExecutor.class,
				element
		);
		builder.addPropertyValue("name", id);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "backlog", "backlog");

		return builder.getBeanDefinition();
	}

}
