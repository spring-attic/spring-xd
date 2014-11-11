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

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.hadoop.store.dataset.DatasetOperations;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;

/**
 * @author Thomas Risberg
 * @author David Turanski
 */
public class DatasetOutboundChannelAdapterIntegrationTests {

	@Before
	public void beforeMethod() {
		//This test disabled for Windows. Getting NPE in ProcessBuilder.start() called from org.apache.util.Shell
		org.junit.Assume.assumeFalse(System.getProperty("os.name").toLowerCase().startsWith("windows"));
	}

	@Test
	public void testWritingDataset() throws Exception {

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/xd/integration/hadoop/config/DatasetOutboundChannelAdapterIntegrationTests.xml");
		MessageChannel channel = context.getBean("datasetOut", MessageChannel.class);
		channel.send(MessageBuilder.withPayload("foo").build());
		channel.send(MessageBuilder.withPayload("bar").build());
		DatasetOperations datasetOperations = context.getBean("datasetOperations", DatasetOperations.class);
		String path = context.getBean("path", String.class);
		assertTrue("Dataset path created", new File(path).exists());
		String fileSeparator = File.separator;
		assertTrue("Dataset storage created",
				new File(path + "/" + "test/" + datasetOperations.getDatasetName(String.class)).exists());
		assertTrue("Dataset metadata created",
				new File(path + "/" + "test/" + datasetOperations.getDatasetName(String.class) + "/.metadata").exists());
		context.close();
	}

}
