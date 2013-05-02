/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.module.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.SubscribableChannel;

/**
 * @author David Turanski
 */
public class ChannelExporterTests {

	@Test
	public void testBasicDefaultChannelExporter() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("/org/springframework/integration/module/config/test-channels.xml");
		Map<String,MessageChannel> channels = ctx.getBeansOfType(MessageChannel.class);
		ChannelExporter channelExporter = new DefaultChannelExporter();
		assertNotNull(channelExporter.getInputChannel(channels));
		assertEquals(1,channelExporter.getOutputChannels(channels).size());
		ctx.close();
	}

	@Test
	public void testMultipleOutputChannels() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("/org/springframework/integration/module/config/multiple-output-channels.xml");
		ChannelExporter channelExporter = new DefaultChannelExporter();
		Map<String,MessageChannel> channels = ctx.getBeansOfType(MessageChannel.class);
		assertNotNull(channelExporter.getInputChannel(channels));
		assertEquals(3,channelExporter.getOutputChannels(channels,SubscribableChannel.class).size());
		ctx.close();
	}

	@Test
	public void testNoExportedChannels() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("/org/springframework/integration/module/config/no-exported-channels.xml");
		ChannelExporter channelExporter = new DefaultChannelExporter();
		Map<String,MessageChannel> channels = ctx.getBeansOfType(MessageChannel.class);
		assertNull(channelExporter.getInputChannel(channels));
		assertEquals(0,channelExporter.getOutputChannels(channels).size());
		ctx.close();
	}

}
