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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.integration.reactor.net.NetServerInboundChannelAdapter;
import reactor.core.Environment;
import reactor.net.tcp.support.SocketUtils;
import reactor.spring.context.config.EnableReactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * @author Jon Brisbin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class NetServerInboundChannelAdapterTests {

	@Autowired
	Executor                executor;
	@Autowired
	PublishSubscribeChannel input;
	@Autowired
	StringWriter            writer;

	@Test
	public void netServerIngestsMessages() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(2);
		input.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				if("Hello World!".equals(message.getPayload())) {
					latch.countDown();
				}
			}
		});

		executor.execute(writer);

		assertTrue("latch was counted down", latch.await(5, TimeUnit.SECONDS));
	}


	@Configuration
	@EnableReactor
	static class TestConfiguration {

		int port = SocketUtils.findAvailableTcpPort();

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
		public NetServerInboundChannelAdapter netServerInboundChannelAdapter(Environment env,
		                                                                     MessageChannel output) {
			NetServerInboundChannelAdapter ica = new NetServerInboundChannelAdapter(env);
			ica.setPort(port);
			ica.setOutputChannel(output);
			ica.setCodec("string");
			ica.setFraming("delimited");
			return ica;
		}
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
				SocketChannel ch = SocketChannel.open();
				ch.configureBlocking(true);
				ch.connect(new InetSocketAddress(port));
				ch.write(ByteBuffer.wrap(line.getBytes()));
				ch.write(ByteBuffer.wrap(line.getBytes()));
				ch.close();
			} catch(IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}

}
