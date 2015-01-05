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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.text.SimpleDateFormat;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.hadoop.store.dataset.DatasetOperations;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.xd.test.TestPojo;

/**
 * Tests the {@code int-hadoop:dataset-outbound-channel-adapter} backed by a {@lnk DatasetWritingMessageHandlerFactoryBean}
 * 
 * @author Thomas Risberg
 */
public class DatasetOutboundChannelAdapterPartitionedIntegrationTests {

	@Test
	public void testWritingDataset() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/xd/integration/hadoop/config/DatasetOutboundChannelAdapterPartitionedIntegrationTests.xml");
		MessageChannel channel = context.getBean("datasetOut", MessageChannel.class);
		TestPojo t1 = new TestPojo();
		t1.setId(1);
		t1.setTimestamp(System.currentTimeMillis());
		t1.setDescription("foo");
		channel.send(MessageBuilder.withPayload(t1).build());
		TestPojo t2 = new TestPojo();
		t2.setId(2);
		t2.setTimestamp(System.currentTimeMillis());
		channel.send(MessageBuilder.withPayload(t2).build());
		DatasetOperations datasetOperations = context.getBean("datasetOperations", DatasetOperations.class);
		String path = context.getBean("path", String.class);
		assertTrue("Dataset path created", new File(path).exists());
		assertTrue("Dataset storage created",
				new File(path + "/" + datasetOperations.getDatasetName(TestPojo.class)).exists());
		assertTrue("Dataset metadata created",
				new File(path + "/" + datasetOperations.getDatasetName(TestPojo.class) + "/.metadata").exists());
		String year = new SimpleDateFormat("yyyy").format(t1.getTimestamp());
		assertTrue("Dataset partition path created",
				new File(path + "/" + datasetOperations.getDatasetName(TestPojo.class) + "/year=" + year).exists());
		context.close();
	}

}
