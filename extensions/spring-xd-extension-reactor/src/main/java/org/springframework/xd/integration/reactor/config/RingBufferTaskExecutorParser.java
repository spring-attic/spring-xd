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
