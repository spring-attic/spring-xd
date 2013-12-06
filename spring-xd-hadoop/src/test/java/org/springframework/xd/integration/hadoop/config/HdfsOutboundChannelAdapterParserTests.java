/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.integration.hadoop.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.hadoop.conf.Configuration;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.hadoop.store.output.TextFileWriter;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.xd.integration.hadoop.outbound.HdfsStoreMessageHandler;

/**
 * @author Mark Fisher
 * @author Janne Valkealahti
 */
public class HdfsOutboundChannelAdapterParserTests {

	@Test
	public void test() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/xd/integration/hadoop/config/HdfsOutboundChannelAdapterParserTests.xml");
		EventDrivenConsumer adapter = context.getBean("adapter", EventDrivenConsumer.class);
		HdfsStoreMessageHandler handler = (HdfsStoreMessageHandler) new DirectFieldAccessor(adapter).getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(false, handlerAccessor.getPropertyValue("autoStartup"));

		TextFileWriter storeWriter = (TextFileWriter) handlerAccessor.getPropertyValue("storeWriter");
		assertNotNull(storeWriter);

		Configuration configuration = (Configuration) new DirectFieldAccessor(storeWriter).getPropertyValue("configuration");
		assertEquals(context.getBean("hadoopConfiguration"), configuration);

		context.close();
	}

}
