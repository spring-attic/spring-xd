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

package org.springframework.xd.integration.reactor;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.SocketUtils;
import org.springframework.validation.BindException;
import org.springframework.xd.integration.reactor.net.ReactorPeerInboundChannelAdapterConfiguration;
import org.springframework.xd.integration.reactor.net.ReactorNetSourceOptionsMetadata;
import org.springframework.xd.module.options.PojoModuleOptionsMetadata;

/**
 * @author Jon Brisbin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = NetServerInboundChannelAdapterIntegrationTests.BaseConfig.class)
public class NetServerInboundChannelAdapterIntegrationTests {

	@Autowired
	Executor executor;

	@Autowired
	PublishSubscribeChannel output;

	@Autowired
	StringWriter writer;

	@Test
	public void netServerIngestsMessages() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(2);
		output.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				if ("Hello World!".equals(message.getPayload())) {
					latch.countDown();
				}
			}
		});

		executor.execute(writer);

		assertTrue("latch was counted down", latch.await(5, TimeUnit.SECONDS));
	}

	@Configuration
	static class TestConfig {

		int port = SocketUtils.findAvailableTcpPort();

		@Bean
		public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() throws BindException {
			PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();
			MutablePropertySources ps = new MutablePropertySources();
			ps.addFirst(optionsMetadataPropertySource());
			pspc.setPropertySources(ps);
			return pspc;
		}

		@Bean
		public PropertySource<?> optionsMetadataPropertySource() throws BindException {
			Map<String, String> opts = new HashMap<String, String>();
			opts.put("port", String.valueOf(port));

			return new PojoModuleOptionsMetadata(ReactorNetSourceOptionsMetadata.class)
					.interpolate(opts)
					.asPropertySource();
		}

		@Bean
		public StringWriter writer() {
			return new StringWriter(port);
		}

		@Bean
		public Executor executor() {
			return Executors.newCachedThreadPool();
		}

		@Bean
		public PublishSubscribeChannel output() {
			return new PublishSubscribeChannel();
		}

		@Bean
		public PublishSubscribeChannel errorChannel() {
			return new PublishSubscribeChannel();
		}

	}

	@Configuration
	@Import({
			TestConfig.class,
			ReactorPeerInboundChannelAdapterConfiguration.class
	})
	static class BaseConfig {
	}

	static class StringWriter implements Runnable {

		final String line = "Hello World!\n";

		final int port;

		StringWriter(int port) {
			this.port = port;
		}

		@Override
		public void run() {
			try {
				InetSocketAddress connectAddr = new InetSocketAddress("127.0.0.1", port);
				SocketChannel ch = SocketChannel.open();
				ch.configureBlocking(true);
				ch.connect(connectAddr);
				ch.write(ByteBuffer.wrap(line.getBytes()));
				ch.write(ByteBuffer.wrap(line.getBytes()));
				ch.close();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}

}
