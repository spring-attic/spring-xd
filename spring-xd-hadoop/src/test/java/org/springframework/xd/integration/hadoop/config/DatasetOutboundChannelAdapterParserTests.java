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

import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.hadoop.store.dataset.DatasetOperations;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.xd.hadoop.fs.DatasetWriterFactory;
import org.springframework.xd.integration.hadoop.outbound.HdfsWritingMessageHandler;

import static org.junit.Assert.assertEquals;

/**
 * @author Thomas Risberg
 */
public class DatasetOutboundChannelAdapterParserTests {

	@Test
	public void testParser() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/xd/integration/hadoop/config/DatasetOutboundChannelAdapterParserTests.xml");
		EventDrivenConsumer adapter = context.getBean("adapter", EventDrivenConsumer.class);
		HdfsWritingMessageHandler handler = (HdfsWritingMessageHandler) new DirectFieldAccessor(adapter).getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(false, handlerAccessor.getPropertyValue("autoStartup"));
		DatasetWriterFactory writerFactory = (DatasetWriterFactory) handlerAccessor.getPropertyValue("hdfsWriterFactory");
		DatasetOperations datasetOperations = (DatasetOperations) new DirectFieldAccessor(writerFactory).getPropertyValue("datasetOperations");
		assertEquals(context.getBean("datasetOperations"), datasetOperations);
		context.close();
	}

}
