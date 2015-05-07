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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.integration.reactor.syslog.SyslogInboundChannelAdapter;
import org.springframework.xd.integration.reactor.syslog.SyslogInboundChannelAdapterConfiguration;
import reactor.Environment;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * @author Jon Brisbin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class SyslogInboundChannelAdapterIntegrationTests {

	static int MSG_COUNT = 2000;

	CountDownLatch latch;

	Environment env;

	long start;

	long end;

	double elapsed;

	@Autowired
	SyslogInboundChannelAdapter channelAdapter;

	@Autowired
	SyslogWriter syslogWriter1;

	@Autowired
	SyslogWriter syslogWriter2;

	@Autowired
	SyslogWriter syslogWriter3;

	@Autowired
	SyslogWriter syslogWriter4;

	@Autowired
	DirectChannel output;

	@Before
	public void setup() {
		env = new Environment();
		latch = new CountDownLatch(MSG_COUNT * 4);
		output.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				latch.countDown();
			}
		});
	}

	private void start(String name) {
		System.out.println("Starting " + name + " test...");
		start = System.currentTimeMillis();
	}

	private void stop() {
		end = System.currentTimeMillis();
		elapsed = (end - start);

		long throughput = Math.round((MSG_COUNT * 4) / (elapsed / 1000));
		System.out.println("Sent " + (MSG_COUNT * 4) + " msgs in " + Math.round(elapsed) + "ms. Throughput: " +
				throughput +
				"/sec");
	}

	@Test
	public void testSyslogInboundChannelAdapter() throws InterruptedException {
		start("syslog");
		syslogWriter1.start();
		syslogWriter2.start();
		syslogWriter3.start();
		syslogWriter4.start();
		assertTrue("Latch did not time out", latch.await(120, TimeUnit.SECONDS));
		stop();
	}

	@Configuration
	@Import(SyslogInboundChannelAdapterConfiguration.class)
	static class TestConfiguration {

		static
		@Bean
		public PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
			return new PropertyPlaceholderConfigurer();
		}

		@Value("${transport:tcp}")
		String transport;

		@Bean
		public DirectChannel output() {
			return new DirectChannel();
		}

		@Bean
		public SyslogWriter syslogWriter1() {
			return new SyslogWriter(transport, MSG_COUNT);
		}

		@Bean
		public SyslogWriter syslogWriter2() {
			return new SyslogWriter(transport, MSG_COUNT);
		}

		@Bean
		public SyslogWriter syslogWriter3() {
			return new SyslogWriter(transport, MSG_COUNT);
		}

		@Bean
		public SyslogWriter syslogWriter4() {
			return new SyslogWriter(transport, MSG_COUNT);
		}
	}

	static class SyslogWriter extends Thread {

		final int runs;

		final String transport;

		SyslogWriter() {
			this("tcp", 1);
		}

		SyslogWriter(String transport, int runs) {
			super();
			this.transport = transport;
			this.runs = runs;
		}

		@Override
		public void run() {
			try {
				InetSocketAddress connectAddr = new InetSocketAddress("127.0.0.1", 5140);

				ByteChannel out;
				if ("tcp".equals(transport)) {
					SocketChannel channel = SocketChannel.open();
					channel.connect(connectAddr);
					out = channel;
				}
				else {
					DatagramChannel channel = DatagramChannel.open();
					channel.connect(connectAddr);
					out = channel;
				}

				ByteBuffer buff = ByteBuffer.wrap(
						("<34>Oct 11 22:14:15 mymachine su: 'su root' failed for lonvick on /dev/pts/8\n").getBytes()
				);
				for (int i = 0; i < runs; i++) {
					out.write(buff);
					buff.flip();
					if ("udp".equals(transport)) {
						Thread.sleep(5);
					}
				}

				out.close();
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}

	}

}
