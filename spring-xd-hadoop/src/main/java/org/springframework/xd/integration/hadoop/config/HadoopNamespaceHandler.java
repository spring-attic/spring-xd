/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.integration.hadoop.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for Spring Integration's 'hadoop' namespace.
 * 
 * @author Mark Fisher
 * @author Thomas Risberg
 */
public class HadoopNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	@Override
	public void init() {
		registerBeanDefinitionParser("store-writer", new StoreWriterParser());
		registerBeanDefinitionParser("naming-strategy", new NamingStrategyParser());
		registerBeanDefinitionParser("rollover-strategy", new RolloverStrategyParser());
		registerBeanDefinitionParser("hdfs-outbound-channel-adapter", new HdfsOutboundChannelAdapterParser());
		registerBeanDefinitionParser("dataset-outbound-channel-adapter", new DatasetOutboundChannelAdapterParser());
	}

}
