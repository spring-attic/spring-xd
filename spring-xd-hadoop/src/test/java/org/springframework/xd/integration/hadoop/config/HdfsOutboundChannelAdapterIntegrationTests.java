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
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;

/**
 * @author Mark Fisher
 * @author Thomas Risberg
 */
public class HdfsOutboundChannelAdapterIntegrationTests {

	@Test
	public void test() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/xd/integration/hadoop/config/HdfsOutboundChannelAdapterIntegrationTests.xml");
		MessageChannel channel = context.getBean("hdfsOut", MessageChannel.class);
		channel.send(MessageBuilder.withPayload("foo").build());
		channel.send(MessageBuilder.withPayload("bar").build());
		FileSystem fileSystem = context.getBean("hadoopFs", FileSystem.class);
		String path = context.getBean("path", String.class);
		context.close();
		Path basepath = new Path(path + "/testdir/");
		Path filepath0 = new Path(basepath, "testfile0");
		Path filepath1 = new Path(basepath, "testfile1");
		assertTrue(fileSystem.exists(basepath));
		assertTrue(fileSystem.exists(filepath0));
		assertTrue(fileSystem.exists(filepath1));
		BufferedReader reader0 = new BufferedReader(new InputStreamReader(fileSystem.open(filepath0)));
		assertEquals("foo", reader0.readLine());
		BufferedReader reader1 = new BufferedReader(new InputStreamReader(fileSystem.open(filepath1)));
		assertEquals("bar", reader1.readLine());
		reader0.close();
		reader1.close();
		assertTrue(fileSystem.delete(basepath, true));
	}

}
